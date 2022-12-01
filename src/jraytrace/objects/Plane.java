package jraytrace.objects;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jraytrace.*;

/**
 *
 * @author ramin
 */
public class Plane extends RealObject {
    private final Vector3 v1,v2 ;
    private final Vector3 normal ;
    private final float dot;
    private final float invlen12,invlen22 ;

    // actually, it's a parallelogram, not an infinite plane
    public Plane(String name, Vector3 location, Vector3 v1, Vector3 v2, Material material) {
        super(name,location,material) ;
        this.v1=v1 ;
        this.v2=v2 ;
        normal=v1.cross(v2).normalize() ;
        dot=normal.dotprod(location) ;
        invlen12=1.0f/v1.lengthSquared() ;
        invlen22=1.0f/v2.lengthSquared() ;
    }

    @Override
    public VectorMask<Float> isCollision(RayPacket rays) {
        FloatVector rayOriginX=rays.originX();
        FloatVector rayOriginY=rays.originY();
        FloatVector rayOriginZ=rays.originZ();
        FloatVector rayDirX=rays.dirX();
        FloatVector rayDirY=rays.dirY();
        FloatVector rayDirZ=rays.dirZ();

        FloatVector normalX = SIMD.create(normal.x);
        FloatVector normalY = SIMD.create(normal.y);
        FloatVector normalZ = SIMD.create(normal.z);

        // d = normal _dotprod_ rayDirection
        FloatVector d = rayDirX.fma(normalX, rayDirY.fma(normalY, rayDirZ.mul(normalZ)));
        // if d is 0.0, it means we are looking parallel to the plane and we will not hit it

        // hitDistance = (dot - normal _dotprod_ rayOrigin) / d
        FloatVector hitDistance = SIMD.create(dot).sub(rayOriginX.fma(normalX, rayOriginY.fma(normalY, rayOriginZ.mul(normalZ)))).div(d);
        // if d is negative (or close to zero if self hit) or >= minDistance there is no hit

        // quick check whether the plane is parallel to the ray or behind us
        VectorMask<Float> mask = hitDistance.test(VectorOperators.IS_INFINITE).or(hitDistance.lt(SIMD.EPSILON));
        if(mask.allTrue()) {
            return mask.not();
        }

        // hitLocation = rayOrigin + rayDir*hitDistance
        FloatVector hitLocationX=rayDirX.fma(hitDistance, rayOriginX);
        FloatVector hitLocationY=rayDirY.fma(hitDistance, rayOriginY);
        FloatVector hitLocationZ=rayDirZ.fma(hitDistance, rayOriginZ);

        // delta = hitLocation - objectPosition
        FloatVector deltaX=hitLocationX.sub(position.x);
        FloatVector deltaY=hitLocationY.sub(position.y);
        FloatVector deltaZ=hitLocationZ.sub(position.z);

        FloatVector u=deltaX.mul(v1.x).add(deltaY.mul(v1.y)).add(deltaZ.mul(v1.z)).mul(invlen12);
        FloatVector v=deltaX.mul(v2.x).add(deltaY.mul(v2.y)).add(deltaZ.mul(v2.z)).mul(invlen22);

        return mask
                .or(u.test(VectorOperators.IS_NEGATIVE))  // u<0
                .or(u.compare(VectorOperators.GT, 1.0f))     // u>1
                .or(v.test(VectorOperators.IS_NEGATIVE))  // v<0
                .or(v.compare(VectorOperators.GT, 1.0f))     // v>1
                .not();
    }

    @Override
    public void findCollision(RayPacket rays, HitInfoPacket hitInfo) {
        FloatVector rayOriginX=rays.originX();
        FloatVector rayOriginY=rays.originY();
        FloatVector rayOriginZ=rays.originZ();
        FloatVector rayDirX=rays.dirX();
        FloatVector rayDirY=rays.dirY();
        FloatVector rayDirZ=rays.dirZ();

        FloatVector normalX = SIMD.create(normal.x);
        FloatVector normalY = SIMD.create(normal.y);
        FloatVector normalZ = SIMD.create(normal.z);

        // d = normal _dotprod_ rayDirection
        FloatVector d = rayDirX.fma(normalX, rayDirY.fma(normalY, rayDirZ.mul(normalZ)));
        // if d is 0.0, it means we are looking parallel to the plane and we will not hit it

        // hitDistance = (dot - normal _dotprod_ rayOrigin) / d
        FloatVector hitDistance = SIMD.create(dot).sub(rayOriginX.fma(normalX, rayOriginY.fma(normalY, rayOriginZ.mul(normalZ)))).div(d);
        // if d is negative (or close to zero if self hit) or >= minDistance there is no hit

        // quick check whether the plane is parallel to the rays or behind us or too far away
        FloatVector minHitDistance = SIMD.fromArray(hitInfo.distance,0);
        VectorMask<Float> notHitMask =
                    hitDistance.test(VectorOperators.IS_INFINITE)
                .or(hitDistance.lt(SIMD.EPSILON))
                .or(hitDistance.compare(VectorOperators.GE, minHitDistance));
        if(notHitMask.allTrue()) {
            return;
        }

        // hitLocation = rayOrigin + rayDir*hitDistance
        FloatVector hitLocationX = rayDirX.fma(hitDistance, rayOriginX);
        FloatVector hitLocationY = rayDirY.fma(hitDistance, rayOriginY);
        FloatVector hitLocationZ = rayDirZ.fma(hitDistance, rayOriginZ);

        // delta = hitLocation - objectPosition
        FloatVector deltaX = hitLocationX.sub(position.x);
        FloatVector deltaY = hitLocationY.sub(position.y);
        FloatVector deltaZ = hitLocationZ.sub(position.z);

        // u = (delta _dotprod_ v1) / len12
        FloatVector u = deltaX.mul(v1.x).add(deltaY.mul(v1.y)).add(deltaZ.mul(v1.z)).mul(invlen12);
        // v = (delta _dotprod_ v2) / len22
        FloatVector v = deltaX.mul(v2.x).add(deltaY.mul(v2.y)).add(deltaZ.mul(v2.z)).mul(invlen22);

        notHitMask = notHitMask
                .or(u.test(VectorOperators.IS_NEGATIVE))      // u<0
                .or(u.compare(VectorOperators.GT, 1.0f))     // u>1
                .or(v.test(VectorOperators.IS_NEGATIVE))      // v<0
                .or(v.compare(VectorOperators.GT, 1.0f));    // v>1
        if(notHitMask.allTrue()) {
            return;
        }
        
        // write result       
        VectorMask<Float> hitMask = notHitMask.not();
        long hitMaskBits = hitMask.toLong();
        hitInfo.hitMask |= hitMaskBits;
        hitDistance.intoArray(hitInfo.distance, 0, hitMask);
        hitLocationX.intoArray(hitInfo.location, 0, hitMask);
        hitLocationY.intoArray(hitInfo.location, RayPacket.SIZE, hitMask);
        hitLocationZ.intoArray(hitInfo.location, RayPacket.SIZE*2, hitMask);
        normalX.intoArray(hitInfo.normal, 0, hitMask);
        normalY.intoArray(hitInfo.normal, RayPacket.SIZE, hitMask);
        normalZ.intoArray(hitInfo.normal, RayPacket.SIZE*2, hitMask);
        u.intoArray(hitInfo.uv, 0, hitMask);
        v.intoArray(hitInfo.uv, RayPacket.SIZE, hitMask);
        for(int i=0;i<RayPacket.SIZE;i++) {
            if((hitMaskBits & (1<<i))!=0) {
                hitInfo.object[i] = this;
            }
        }
    }
}

