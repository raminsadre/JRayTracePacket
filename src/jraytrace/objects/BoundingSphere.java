package jraytrace.objects;

import java.util.Arrays;
import java.util.List;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jraytrace.HitInfoPacket;
import jraytrace.RayPacket;
import jraytrace.SIMD;
import jraytrace.Vector3;

/**
 *
 * @author ramin
 */
public class BoundingSphere extends Sphere {
    private final List<RealObject> objects;

    public BoundingSphere(String name, Vector3 location, float radius, RealObject[] objects) {
        super(name, location, radius, null);
        this.objects = Arrays.asList(objects);
    }

    @Override
    public VectorMask<Float> isCollision(RayPacket rays) {
        // first, test whether the bounding sphere was hit at all
        final VectorMask<Float> isBoundingHit = super.isCollision(rays);
        if(isBoundingHit.anyTrue()) {
            VectorMask<Float> result = VectorMask.fromLong(SIMD.SPECIES, 0);
            for(RealObject obj : objects) {          
                result = result.or(obj.isCollision(rays));
                // all rays have reported a hit? -> stop searching
                if(result.allTrue()) {
                    break;
                }
            }
            return result; 
        }
        else {
            return isBoundingHit;
        }
    }

    @Override
    public void findCollision(RayPacket rays, HitInfoPacket hitInfo) {      
        // first, test whether the bounding sphere was hit at all
        final FloatVector hitDistance = getCollisionDistance(rays);
        final VectorMask<Float> notHitMask =
                    hitDistance.test(VectorOperators.IS_NAN)
                .or(hitDistance.test(VectorOperators.IS_NEGATIVE));
        if(notHitMask.allTrue()) {
            return;
        }
        
        // now check the objects inside the bounding sphere
        for(RealObject obj : objects) {
            obj.findCollision(rays, hitInfo) ;
        }        
    }
}
