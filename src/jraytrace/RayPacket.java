package jraytrace;

import jdk.incubator.vector.FloatVector;

public class RayPacket {
    public static final int SIZE = SIMD.SPECIES.length();

    // layout:
    //   x0 x1 ... x_3  y0 y1 ... y_3 z0 z1 ... z_3
    public final float[] origins;
    public final float[] dirs;
    
    public RayPacket(float[] origins) {
        this.origins = origins;
        dirs = new float[SIZE*3];
    }

    public RayPacket(Vector3 commonOrigin) {
        origins = new float[SIZE*3];
        dirs = new float[SIZE*3];
        for(int i=0;i<SIZE;i++) {
            origins[i] = commonOrigin.x;
            origins[i+SIZE] = commonOrigin.y;
            origins[i+SIZE*2] = commonOrigin.z;
        }
    }

    public Vector3 getDirection(int rayIndex) {
        return new Vector3(dirs[rayIndex], dirs[rayIndex+SIZE], dirs[rayIndex+SIZE*2]);
    }

    public void setDirection(int rayIndex, Vector3 newDirection) {
        dirs[rayIndex] = newDirection.x;
        dirs[rayIndex+SIZE] = newDirection.y;
        dirs[rayIndex+SIZE*2] = newDirection.z;
    }

    public void setCommonDirection(Vector3 newDirection) {
        for(int i=0;i<SIZE;i++) {
            dirs[i]=newDirection.x;
            dirs[i+SIZE]=newDirection.y;
            dirs[i+SIZE*2]=newDirection.z;
        }
    }

    public FloatVector originX() {
        return SIMD.fromArray(origins,0);
    }

    public FloatVector originY() {
        return SIMD.fromArray(origins,SIZE);
    }

    public FloatVector originZ() {
        return SIMD.fromArray(origins,SIZE*2);
    }

    public FloatVector dirX() {
        return SIMD.fromArray(dirs,0);
    }

    public FloatVector dirY() {
        return SIMD.fromArray(dirs,SIZE);
    }

    public FloatVector dirZ() {
        return SIMD.fromArray(dirs,SIZE*2);
    }
}
