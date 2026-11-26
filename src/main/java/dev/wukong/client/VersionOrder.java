package dev.wukong.client;
import java.math.BigInteger;
import java.util.regex.Pattern;
/** Compare semantic release tags without treating arbitrary tags as updates. */
final class VersionOrder {
    private static final Pattern P=Pattern.compile("^[vV]?([0-9]+)\\.([0-9]+)\\.([0-9]+)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$");
    static Integer compare(String left,String right){
        var l=P.matcher(left);var r=P.matcher(right);if(!l.matches()||!r.matches())return null;
        for(int i=1;i<=3;i++){int n=new BigInteger(l.group(i)).compareTo(new BigInteger(r.group(i)));if(n!=0)return n;}
        String a=l.group(4),b=r.group(4);if(a==null||b==null)return a==null?(b==null?0:1):-1;
        var x=a.split("\\.");var y=b.split("\\.");
        for(int i=0;i<Math.min(x.length,y.length);i++){
            boolean xn=x[i].matches("[0-9]+"),yn=y[i].matches("[0-9]+");
            int n=xn&&yn?new BigInteger(x[i]).compareTo(new BigInteger(y[i])):xn?-1:yn?1:x[i].compareTo(y[i]);
            if(x[i].equals(y[i]))n=0;if(n!=0)return n;
        }return Integer.compare(x.length,y.length);
    }
    private VersionOrder(){}
}
