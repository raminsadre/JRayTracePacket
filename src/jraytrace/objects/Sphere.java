package jraytrace.objects;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jraytrace.*;

/**
 *
 * @author ramin
 */
public class Sphere extends RealObject {
    private final float invRadius ;
    private final float radius2 ;

    public Sphere(String name, Vector3 position, float radius, Material material) {
        super(name,position,material) ;
        this.invRadius=1.0f/radius;
        this.radius2=radius*radius ;
    }

    @Override
    public VectorMask<Float> isCollision(RayPacket rays) {
        FloatVector hitDistance = getCollisionDistance(rays);
        // test whether distance is >=0
        return hitDistance.test(VectorOperators.IS_NAN).or(hitDistance.lt(SIMD.EPSILON)).not();
    }
    
    // returns null if not hit
    @Override
    public void findCollision(RayPacket rays, HitInfoPacket hitInfo) {
        FloatVector hitDistance = getCollisionDistance(rays);

        // sphere not hit by any ray or all hits are farer than the current minimum? -> stop here
        FloatVector minHitDistance = SIMD.fromArray(hitInfo.distance,0);
        VectorMask<Float> notHitMask =
                    hitDistance.test(VectorOperators.IS_NAN)
                .or(hitDistance.lt(SIMD.EPSILON))
                .or(hitDistance.compare(VectorOperators.GE, minHitDistance));
        if(notHitMask.allTrue()) {
            return;
        }

        // hitLocation = rayOrigin + rayDir*hitDistance
        FloatVector hitLocationX = rays.dirX().fma(hitDistance, rays.originX());
        FloatVector hitLocationY = rays.dirY().fma(hitDistance, rays.originY());
        FloatVector hitLocationZ = rays.dirZ().fma(hitDistance, rays.originZ());

        // hitNormal = (hitLocation-objectPosition)/objectRadius
        FloatVector vInvRadius = SIMD.create(invRadius);
        FloatVector hitNormalX = hitLocationX.sub(position.x).mul(vInvRadius);
        FloatVector hitNormalY = hitLocationY.sub(position.y).mul(vInvRadius);
        FloatVector hitNormalZ = hitLocationZ.sub(position.z).mul(vInvRadius);

        // write result
        VectorMask<Float> hitMask = notHitMask.not();
        hitInfo.hitMask |= hitMask.toLong();
        hitDistance.intoArray(hitInfo.distance, 0, hitMask);
        hitLocationX.intoArray(hitInfo.location, 0, hitMask);
        hitLocationY.intoArray(hitInfo.location, RayPacket.SIZE, hitMask);
        hitLocationZ.intoArray(hitInfo.location, RayPacket.SIZE*2, hitMask);
        hitNormalX.intoArray(hitInfo.normal, 0, hitMask);
        hitNormalY.intoArray(hitInfo.normal, RayPacket.SIZE, hitMask);
        hitNormalZ.intoArray(hitInfo.normal, RayPacket.SIZE*2, hitMask);
        SIMD.ZERO.intoArray(hitInfo.uv, 0, hitMask);
        SIMD.ZERO.intoArray(hitInfo.uv, RayPacket.SIZE, hitMask);
        for(int i=0;i<RayPacket.SIZE;i++) {
            if(hitMask.laneIsSet(i)) {
                hitInfo.object[i] = this;
            }
        }
    }

    private static final FloatVector negativeVector=SIMD.create(-1);

    // returns collision distance.
    // result is negative or NaN if no collision in front of us.
    protected FloatVector getCollisionDistance(RayPacket rays) {
        // offset = objectPosition - rayOrigin
        FloatVector offsetX = SIMD.create(position.x).sub(rays.originX());
        FloatVector offsetY = SIMD.create(position.y).sub(rays.originY());
        FloatVector offsetZ = SIMD.create(position.z).sub(rays.originZ());

        // b = offset _dotprod_ rayDirection
        FloatVector b = offsetX.fma(rays.dirX(), offsetY.fma(rays.dirY(), offsetZ.mul(rays.dirZ())));

        // lengthSquared = offset _dotprod offset
        FloatVector lengthSquared = offsetX.fma(offsetX, offsetY.fma(offsetY, offsetZ.mul(offsetZ)));

        // d = radius2 - lengthSquared + b*b
        FloatVector d = b.mul(b).add(radius2).sub(lengthSquared);

        // negative d means that we have missed the sphere
        if(d.test(VectorOperators.IS_NEGATIVE).allTrue()) {
            return negativeVector;
        }

        // we do the sqrt anyway, giving NaN if d is negative
        FloatVector sqrtD = d.sqrt();

        // the sphere has been hit at two points at distance b-sqrtD and b+sqrtD
        // we have to find the closest one that is >0 (i.e. in front of us)
        FloatVector distance1 = b.sub(sqrtD);
        return distance1.add(sqrtD.add(sqrtD), distance1.lt(SIMD.EPSILON));
    }
}
