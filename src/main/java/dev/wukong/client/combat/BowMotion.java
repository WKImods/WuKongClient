package dev.wukong.client.combat;

/** Minecraft entity observations in blocks per client tick. No network or game writes. */
public final class BowMotion {
    public record V(double x, double y, double z) {
        public static final V ZERO = new V(0,0,0);
        public V add(V v){return new V(x+v.x,y+v.y,z+v.z);}
        public V sub(V v){return new V(x-v.x,y-v.y,z-v.z);}
        public V mul(double n){return new V(x*n,y*n,z*n);}
        public double length(){return Math.hypot(Math.hypot(x,z),y);}
        public boolean finite(){return Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z);}
        public V limit(double max){double l=length();return l>max?mul(max/l):this;}
    }
    public enum Mode { GROUND, AIR, ELYTRA, WATER, OTHER }
    public record Sample(Mode mode, V velocity, V acceleration, int observations, boolean valid) {
        public static Sample empty(Mode m){return new Sample(m,V.ZERO,V.ZERO,0,false);}
        /** Only short observed acceleration is extrapolated; no assumption about future inputs. */
        public V displacement(double ticks){
            if(!valid || ticks<=0 || !Double.isFinite(ticks)) return V.ZERO;
            double horizon = Math.min(ticks,mode==Mode.ELYTRA?6:mode==Mode.GROUND?3:4);
            double rest=ticks-horizon;
            V adjusted=acceleration.mul(horizon);
            return velocity.mul(ticks).add(adjusted.mul(horizon*.5+rest));
        }
    }
    public static final class Tracker {
        private V last, raw = V.ZERO;
        private int tick;
        private Sample sample=Sample.empty(Mode.OTHER);
        public Sample sample(){return sample;}
        public Sample update(V position,int now,Mode mode){
            if(position==null || !position.finite()) {last=null;sample=Sample.empty(mode);return sample;}
            if(last==null || now<tick || (long)now-tick>5){reset(position,now,mode);return sample;}
            if(now==tick)return sample;
            int elapsed=now-tick;
            V measured=position.sub(last).mul(1.0/elapsed);
            // Large jumps are not reliable motion observations. Flight allows faster legitimate samples.
            double limit=switch(mode){case ELYTRA->5.0;case AIR,OTHER->3.0;case WATER,GROUND->1.8;};
            if(!measured.finite()||measured.length()>limit){reset(position,now,mode);return sample;}
            boolean fresh=!sample.valid()||sample.mode()!=mode;
            double alpha=mode==Mode.ELYTRA?.72:.55;
            V velocity=fresh?measured:sample.velocity().mul(1-alpha).add(measured.mul(alpha));
            double accelLimit=mode==Mode.ELYTRA?.18:mode==Mode.AIR?.12:.10;
            V accel=fresh?V.ZERO:sample.acceleration().mul(.55).add(measured.sub(raw).mul(.45/elapsed)).limit(accelLimit);
            if(mode==Mode.GROUND){velocity=new V(velocity.x(),0,velocity.z());accel=new V(accel.x(),0,accel.z());}
            sample=new Sample(mode,velocity,accel,fresh?1:Math.min(1000,sample.observations()+1),true);
            raw=measured;last=position;tick=now;return sample;
        }
        private void reset(V pos,int now,Mode mode){last=pos;tick=now;raw=V.ZERO;sample=Sample.empty(mode);}
    }
    private BowMotion(){}
}
