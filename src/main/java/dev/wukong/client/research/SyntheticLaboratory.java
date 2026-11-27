package dev.wukong.client.research;

import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

/** Isolated regression laboratory. No game, packet, clock or server dependencies. */
public final class SyntheticLaboratory {
    public static final int SCHEMA = 1;
    public enum Suite { ALL, NORMAL, CAPTURE_QUALITY, OBSERVATION_GAPS, LOCAL_LIMITS }
    public enum Status { RUNNING, COMPLETE, CANCELLED }
    public record Config(long seed, Suite suite, int trials) {
        public Config { Objects.requireNonNull(suite); if(trials<1||trials>1000)throw new IllegalArgumentException("Trials must be 1-1000"); }
    }
    public record Result(int trial, Suite suite, String input, String expected, String actual) {
        public boolean passed(){return expected.equals(actual);}
    }
    public record Report(Config config, Status status, List<Result> results) {
        public Report { results=List.copyOf(results); }
        public long passed(){return results.stream().filter(Result::passed).count();}
        public long failed(){return results.size()-passed();}
    }
    private final ArrayDeque<Report> history=new ArrayDeque<>();
    private Config config;
    private Random random;
    private Status status;
    private final ArrayList<Result> results=new ArrayList<>();
    public void start(Config next){
        if(running())throw new IllegalStateException("Run already active");
        config=Objects.requireNonNull(next); random=new Random(next.seed()); results.clear(); status=Status.RUNNING;
    }
    public boolean running(){return status==Status.RUNNING;}
    public Optional<Report> current(){return config==null?Optional.empty():Optional.of(new Report(config,status,results));}
    public List<Report> history(){return List.copyOf(history);}
    public void step(int budget){
        if(budget<1||budget>64)throw new IllegalArgumentException("Budget must be 1-64");
        if(!running())return;
        for(int j=0;j<budget&&results.size()<config.trials();j++){
            int i=results.size()+1;
            Suite s=config.suite()==Suite.ALL?Suite.values()[1+(i-1)%4]:config.suite();
            // Only fictitious, in-memory snapshots; never use real inventory data.
            int amount=1+random.nextInt(64), next=1+random.nextInt(64), gap=2+random.nextInt(8);
            var d=new DetectionEngine(8);
            String expected=switch(s){
                case NORMAL -> "samples=3;consistent=3;active=none;closed=none";
                case CAPTURE_QUALITY -> "samples=2;consistent=1;active=none;closed=LOCAL_CAPTURE_MISMATCH:CLEARED:1";
                case OBSERVATION_GAPS -> "samples=2;consistent=2;active=none;closed=none";
                case LOCAL_LIMITS -> "samples=2;consistent=1;active=none;closed=REPORTED_STACK_LIMIT:CLEARED:1";
                default -> throw new AssertionError(s);
            };
            String actual;
            try{
                switch(s){
                    case NORMAL -> {feed(d,1,amount,0);feed(d,2,next,0);feed(d,3,amount,0);}
                    case CAPTURE_QUALITY -> {var a=snapshot(amount,0);d.observe(1,a,Map.of(),0);feed(d,2,next,0);}
                    case OBSERVATION_GAPS -> {feed(d,1,amount,0);feed(d,1L+gap,next,0);}
                    case LOCAL_LIMITS -> {feed(d,1,65L+amount,1);feed(d,2,next,0);}
                    default -> throw new AssertionError(s);
                }
                actual="samples="+d.samples()+";consistent="+d.consistentSamples()+";active="+summary(d.active())+";closed="+summary(d.history());
            }catch(RuntimeException e){actual="ERROR: "+e.getClass().getSimpleName();}
            results.add(new Result(i,s,"a="+amount+";b="+next+";gap="+gap,expected,actual));
        }
        if(results.size()==config.trials())finish(Status.COMPLETE);
    }
    private static ResearchLedger.Snapshot snapshot(long n,int invalid){return new ResearchLedger.Snapshot(Map.of("fixture:sample",n),1,invalid,false);}
    private static void feed(DetectionEngine d,long sample,long n,int invalid){var s=snapshot(n,invalid);d.observe(sample,s,s.counts(),s.occupied());}
    private static String summary(List<DetectionEngine.Finding> findings){return findings.isEmpty()?"none":String.join("|",findings.stream().map(f->f.code()+":"+f.state()+":"+f.observations()).toList());}
    public void cancel(){if(running())finish(Status.CANCELLED);}
    private void finish(Status end){status=end;if(history.size()==8)history.removeFirst();history.addLast(current().orElseThrow());}
    public static Path export(Report report,Path directory)throws IOException{
        Objects.requireNonNull(report);if(report.status()==Status.RUNNING)throw new IllegalArgumentException("Finish or cancel first");
        Files.createDirectories(directory);
        // Unique file, no overwrites; only synthetic values are exported.
        Path file=Files.createTempFile(directory,"wukong-lab-",".csv");
        try{
            var b=new StringBuilder("schema,target,seed,suite,requested,status,trial,category,passed,input,expected,actual\n");
            String prefix=SCHEMA+","+CaseLibrary.TARGET+","+report.config().seed()+","+report.config().suite()+","+report.config().trials()+","+report.status()+",";
            if(report.results().isEmpty())b.append(prefix).append(",,,,,\n");
            for(var r:report.results())b.append(prefix).append(r.trial()).append(',').append(r.suite()).append(',').append(r.passed()).append(',').append(quote(r.input())).append(',').append(quote(r.expected())).append(',').append(quote(r.actual())).append('\n');
            Files.writeString(file,b,StandardCharsets.UTF_8,StandardOpenOption.TRUNCATE_EXISTING);
            return file;
        }catch(IOException|RuntimeException e){try{Files.deleteIfExists(file);}catch(IOException cleanup){e.addSuppressed(cleanup);}throw e;}
    }
    private static String quote(String s){return "\""+s.replace("\"","\"\"")+"\"";}
}
