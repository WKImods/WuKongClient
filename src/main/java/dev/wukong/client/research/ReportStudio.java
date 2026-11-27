package dev.wukong.client.research;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;

/** Local-only archive of synthetic test reports. No game or network dependencies. */
public final class ReportStudio {
    private static final String HEADER="schema,target,seed,suite,requested,status,trial,category,passed,input,expected,actual";
    public static final int MAX_FILES=256, MAX_BYTES=2_000_000;
    public record Entry(Path path,String digest,long bytes,boolean duplicate) {}
    public record Index(List<Entry> entries,int skipped,boolean capped) { public Index { entries=List.copyOf(entries); } }
    public record Document(int schema,String target,long seed,String suite,int requested,String status,List<List<String>> rows) {
        public Document { rows=rows.stream().map(List::copyOf).toList(); }
        public long passed(){return rows.stream().filter(r->r.get(8).equals("true")).count();}
        public long failed(){return rows.size()-passed();}
    }
    private final Path dir;
    public ReportStudio(Path directory){dir=directory.toAbsolutePath().normalize();}
    public Path directory(){return dir;}
    public Path ensureDirectory() throws IOException {return Files.createDirectories(dir);}
    private static String hash(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private byte[] bytes(Path file)throws IOException{
        Path p=file.toAbsolutePath().normalize();
        if(!dir.equals(p.getParent())||!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))throw new IOException("Not a regular report in this directory");
        try(var in=Files.newInputStream(p,LinkOption.NOFOLLOW_LINKS)){byte[] b=in.readNBytes(MAX_BYTES+1);if(b.length>MAX_BYTES)throw new IOException("Report exceeds size limit");return b;}
    }
    public Index index()throws IOException{
        ensureDirectory();
        List<Path> files;boolean capped;
        try(Stream<Path> s=Files.list(dir)){
            // Bound the number of inspected files; newest order is within the inspected set.
            files=s.filter(p->p.getFileName().toString().startsWith("wukong-lab-")&&p.getFileName().toString().endsWith(".csv")).limit(MAX_FILES+1L).toList();
        }
        capped=files.size()>MAX_FILES; files=new ArrayList<>(files.subList(0,Math.min(files.size(),MAX_FILES)));
        files.sort(Comparator.comparingLong(ReportStudio::modified).reversed().thenComparing(Path::toString));
        var seen=new HashSet<String>();var entries=new ArrayList<Entry>();int skipped=0;
        for(Path p:files){try{byte[] b=bytes(p);String digest=hash(b);entries.add(new Entry(p,digest,b.length,!seen.add(digest)));}catch(IOException e){skipped++;}}
        return new Index(entries,skipped,capped);
    }
    private static long modified(Path p){try{return Files.getLastModifiedTime(p,LinkOption.NOFOLLOW_LINKS).toMillis();}catch(IOException e){return 0;}}
    public Path save(SyntheticLaboratory.Report report)throws IOException{
        ensureDirectory();
        // Existing exporter is the canonical format. Stage outside the index, then publish.
        Path stage=Files.createTempDirectory(dir,".stage-");Path temp=null;
        try{
            temp=SyntheticLaboratory.export(report,stage);byte[] data=Files.readAllBytes(temp);
            if(data.length==0||data.length>MAX_BYTES)throw new IOException("Invalid exported size");
            Path target=dir.resolve("wukong-lab-"+hash(data)+".csv");
            if(Files.exists(target,LinkOption.NOFOLLOW_LINKS)){
                if(!Arrays.equals(bytes(target),data))throw new IOException("Existing archive content differs");
            }else{
                try{Files.move(temp,target);}catch(FileAlreadyExistsException e){if(!Arrays.equals(bytes(target),data))throw e;}
            }
            if(!Arrays.equals(bytes(target),data))throw new IOException("Archive verification failed");
            return target;
        }finally{if(temp!=null)Files.deleteIfExists(temp);Files.deleteIfExists(stage);}
    }
    public Document read(Path file)throws IOException{
        String data=new String(bytes(file),StandardCharsets.UTF_8);
        var table=parse(data);
        if(table.size()<2||!String.join(",",table.getFirst()).equals(HEADER))throw new IOException("Unknown report header");
        var first=table.get(1);if(first.size()!=12)throw new IOException("Invalid column count");
        try{
            int schema=Integer.parseInt(first.get(0)), requested=Integer.parseInt(first.get(4));long seed=Long.parseLong(first.get(2));
            if(schema!=1||requested<1||requested>1000||!Set.of("COMPLETE","CANCELLED").contains(first.get(5)))throw new IOException("Unsupported report metadata");
            SyntheticLaboratory.Suite.valueOf(first.get(3));
            var rows=new ArrayList<List<String>>();
            for(int i=1;i<table.size();i++){
                var r=table.get(i);if(r.size()!=12||!r.subList(0,6).equals(first.subList(0,6)))throw new IOException("Inconsistent metadata at row "+i);
                if(r.subList(6,12).stream().allMatch(String::isEmpty)&&table.size()==2&&first.get(5).equals("CANCELLED"))continue;
                if(Integer.parseInt(r.get(6))!=rows.size()+1||!Set.of("true","false").contains(r.get(8))||Boolean.parseBoolean(r.get(8))!=r.get(10).equals(r.get(11)))throw new IOException("Invalid result at row "+i);
                SyntheticLaboratory.Suite.valueOf(r.get(7));rows.add(r);
            }
            if(rows.size()>requested||(first.get(5).equals("COMPLETE")&&rows.size()!=requested))throw new IOException("Invalid result count");
            return new Document(schema,first.get(1),seed,first.get(3),requested,first.get(5),rows);
        }catch(IllegalArgumentException e){throw new IOException("Invalid report metadata",e);}
    }
    // RFC-style quoted fields, doubled quotes and embedded newlines; bounded input.
    static List<List<String>> parse(String s)throws IOException{
        var rows=new ArrayList<List<String>>();var row=new ArrayList<String>();var field=new StringBuilder();boolean quoted=false,closed=false;
        for(int i=0;i<s.length();i++){
            char c=s.charAt(i);
            if(quoted){if(c=='"'){if(i+1<s.length()&&s.charAt(i+1)=='"'){field.append('"');i++;}else{quoted=false;closed=true;}}else field.append(c);continue;}
            if(c==','||c=='\n'||c=='\r'){
                row.add(field.toString());field.setLength(0);closed=false;
                if(c!=','){if(c=='\r'&&i+1<s.length()&&s.charAt(i+1)=='\n')i++;rows.add(List.copyOf(row));row.clear();if(rows.size()>1002)throw new IOException("Too many rows");}
                if(row.size()>12)throw new IOException("Too many columns");
            }else if(c=='"'&&field.isEmpty()&&!closed)quoted=true;
            else{if(closed||c=='"')throw new IOException("Invalid CSV quoting");field.append(c);}
        }
        if(quoted)throw new IOException("Unclosed CSV field");
        if(!row.isEmpty()||!field.isEmpty()||closed){row.add(field.toString());rows.add(List.copyOf(row));}
        return rows;
    }
    public Path exportJson(Path file)throws IOException{
        var d=read(file);var b=new StringBuilder("{\n  \"schema\": "+d.schema()+",\n  \"target\": "+json(d.target())+",\n  \"seed\": "+json(Long.toString(d.seed()))+",\n  \"suite\": "+json(d.suite())+",\n  \"requested\": "+d.requested()+",\n  \"status\": "+json(d.status())+",\n  \"results\": [");
        for(int i=0;i<d.rows().size();i++){var r=d.rows().get(i);if(i>0)b.append(',');b.append("\n    {\"trial\": ").append(r.get(6)).append(", \"category\": ").append(json(r.get(7))).append(", \"passed\": ").append(r.get(8)).append(", \"input\": ").append(json(r.get(9))).append(", \"expected\": ").append(json(r.get(10))).append(", \"actual\": ").append(json(r.get(11))).append('}');}
        b.append("\n  ]\n}\n");return writeUnique("wukong-report-",b.toString());
    }
    public Path saveSession(ResearchLedger ledger,DetectionEngine detection)throws IOException{
        var b=new StringBuilder("{\n  \"kind\": \"local-diagnostic-summary\",\n  \"recordedAt\": "+json(java.time.Instant.now().toString())+",\n  \"samples\": "+ledger.samples()+",\n  \"changes\": "+ledger.changed()+",\n  \"discarded\": "+ledger.dropped()+",\n  \"events\": [");
        boolean comma=false;for(var e:ledger.events()){if(comma)b.append(',');comma=true;b.append("\n    {\"sequence\": ").append(e.sequence()).append(", \"time\": ").append(json(e.recordedAt().toString())).append(", \"kind\": ").append(json(e.kind().name())).append('}');}
        b.append("\n  ],\n  \"findings\": [");comma=false;for(var f:Stream.concat(detection.active().stream(),detection.history().stream()).toList()){if(comma)b.append(',');comma=true;b.append("\n    {\"code\": ").append(json(f.code().name())).append(", \"state\": ").append(json(f.state().name())).append(", \"observations\": ").append(f.observations()).append('}');}
        b.append("\n  ]\n}\n");return writeUnique("wukong-session-",b.toString());
    }
    private Path writeUnique(String prefix,String data)throws IOException{ensureDirectory();Path p=Files.createTempFile(dir,prefix,".json");try{Files.writeString(p,data,StandardCharsets.UTF_8,StandardOpenOption.TRUNCATE_EXISTING);if(Files.size(p)!=data.getBytes(StandardCharsets.UTF_8).length)throw new IOException("Write size mismatch");return p;}catch(IOException|RuntimeException e){try{Files.deleteIfExists(p);}catch(IOException cleanup){e.addSuppressed(cleanup);}throw e;}}
    private static String json(String s){var b=new StringBuilder("\"");for(char c:s.toCharArray())switch(c){case '"'->b.append("\\\"");case '\\'->b.append("\\\\");case '\n'->b.append("\\n");case '\r'->b.append("\\r");case '\t'->b.append("\\t");default->{if(c<32)b.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString();}
}
