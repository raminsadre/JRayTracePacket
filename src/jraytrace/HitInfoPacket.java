package jraytrace;

import jraytrace.objects.RealObject;

/**
 *
 * @author ramin
 */
public class HitInfoPacket {
    public long hitMask;                // bit mask indicating whether they ray hit an object
    public final RealObject[] object ;  // the object that was hit
    public final float[] distance ;     // distance from start point of ray to hit location
    public final float[] location ;     // hit location
    public final float[] normal ;       // surface normal at hit location
    public final float[] uv;            // surface coordinates [0,1] (only for planes at the moment)
    
    public HitInfoPacket() {
        object = new RealObject[RayPacket.SIZE];
        distance = new float[RayPacket.SIZE];
        location = new float[RayPacket.SIZE*3];
        normal = new float[RayPacket.SIZE*3];
        uv = new float[RayPacket.SIZE*2];
    }
    
    public boolean isHit(int rayIndex) {
        return (hitMask & (1<<rayIndex)) != 0;
    }
}
