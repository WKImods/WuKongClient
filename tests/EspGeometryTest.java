import dev.wukong.client.EspGeometry;

public class EspGeometryTest {
    private static void check(boolean b, String s) { if(!b) throw new AssertionError(s); }
    private static void near(float a,float b) { check(Math.abs(a-b)<0.001f,"Expected "+b+", got "+a); }
    public static void main(String[] args) {
        var a = EspGeometry.clip(-20,30,120,30,100,60);
        check(a!=null,"horizontal clipping"); near(a.x0(),0); near(a.x1(),100);
        var b = EspGeometry.clip(50,-30,50,80,100,60);
        check(b!=null,"vertical clipping"); near(b.y0(),0); near(b.y1(),60);
        var c = EspGeometry.clip(30,40,70,40,100,60);
        check(c!=null,"inside"); near(c.x0(),30); near(c.x1(),70);
        check(EspGeometry.clip(-4,-4,-3,-3,100,100)==null,"out of view");
        check(EspGeometry.clip(Float.NaN,0,10,10,100,100)==null,"NaN");
        var d = EspGeometry.clip(-25,-25,50,50,100,60);
        check(d!=null,"diagonal"); near(d.x0(),0); near(d.y0(),0);
        System.out.println("PASS: ESP 6 geometry cases");
    }
}
