package jraytrace.objects;

import jdk.incubator.vector.VectorMask;
import jraytrace.HitInfoPacket;
import jraytrace.Material;
import jraytrace.RayPacket;
import jraytrace.Vector3;

/**
 *
 * @author ramin
 */
public abstract class RealObject extends Obj {
    private final Material material ;

    public RealObject(String name, Vector3 position, Material material) {
        super(name,position) ;
        this.material = material;
    }
    
    public Material getMaterial() {
        return material ;
    }

    // returns true if collision, without any details on hit location (used for shadow computation).
    public abstract VectorMask<Float> isCollision(RayPacket rays);
   
    // checks whether the rays intersect with this object. If so and the intresection
    // point is closer than the one stored in hitInfo, the hit info is updated.
    public abstract void findCollision(RayPacket rays, HitInfoPacket hitInfo);
}
