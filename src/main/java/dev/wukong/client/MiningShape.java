package dev.wukong.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure, deterministic geometry shared by the selection, preview and execution. */
final class MiningShape {
    record Offset(int x,int y,int z) {
        int distanceSquared(){return x*x+y*y+z*z;}
    }
    enum Face { NORTH,SOUTH,EAST,WEST,UP,DOWN }
    static int clamp(int value,int min,int max){return Math.max(min,Math.min(max,value));}
    static List<Offset> precision(int width,int height,int depth,Face face){
        width=clamp(width,1,5);height=clamp(height,1,5);depth=clamp(depth,1,3);
        var result=new ArrayList<Offset>(width*height*depth);
        // Even widths extend to the viewer's right, even heights upwards (horizontal faces).
        for(int d=0;d<depth;d++)for(int v=-(height-1)/2;v<=height/2;v++)for(int u=-(width-1)/2;u<=width/2;u++){
            result.add(switch(face){
                case NORTH -> new Offset(-u,v,d);
                case SOUTH -> new Offset(u,v,-d);
                case EAST -> new Offset(-d,v,-u);
                case WEST -> new Offset(d,v,u);
                case UP -> new Offset(u,-d,-v);
                case DOWN -> new Offset(u,d,v);
            });
        }
        return List.copyOf(result);
    }
    static List<Offset> area(int radius,boolean sphere,boolean layer){
        return area(radius,sphere,layer,false);
    }
    // Round up fractional feet positions so slabs/stairs and their entire layer are preserved.
    static int floorStart(double feetY){return (int)Math.ceil(feetY-1.0e-6);}
    static List<Offset> area(int radius,boolean sphere,boolean layer,boolean protectFloor){
        radius=clamp(radius,1,5);var result=new ArrayList<Offset>();
        for(int y=(layer||protectFloor)?0:-radius;y<=(layer?0:radius);y++)for(int z=-radius;z<=radius;z++)for(int x=-radius;x<=radius;x++){
            if(sphere&&x*x+y*y+z*z>radius*radius)continue;
            result.add(new Offset(x,y,z));
        }
        result.sort(Comparator.comparingInt(Offset::distanceSquared).thenComparingInt(Offset::y).thenComparingInt(Offset::x).thenComparingInt(Offset::z));
        return List.copyOf(result);
    }
    private MiningShape(){}
}
