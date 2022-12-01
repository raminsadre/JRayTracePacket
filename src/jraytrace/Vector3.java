package jraytrace;

/**
 *
 * @author ramin
 */
public final class Vector3 {
    public final float x,y,z;

    public Vector3(float x, float y, float z) {
        this.x=x;
        this.y=y;
        this.z=z;
    }

    public Vector3 add(Vector3 v) {
        return new Vector3(x+v.x,y+v.y,z+v.z) ;
    }

    // result = this + f*v
    public Vector3 add(Vector3 v, float f) {
        return new Vector3(x+f*v.x,y+f*v.y,z+f*v.z) ;
    }

    public Vector3 subst(Vector3 v) {
        return new Vector3(x-v.x,y-v.y,z-v.z) ;
    }

    public Vector3 mult(float f) {
        return new Vector3(x*f,y*f,z*f) ;
    }

    public Vector3 normalize() {
        final float invLength=1.0f/length() ;
        return new Vector3(x*invLength,y*invLength,z*invLength) ;
    }

    public float length() {
        return (float)Math.sqrt(x*x+y*y+z*z) ;
    }

    public float lengthSquared() {
        return x*x+y*y+z*z ;
    }

    public Vector3 cross(Vector3 v) {
        return new Vector3(y*v.z-z*v.y,z*v.x-x*v.z,x*v.y-y*v.x) ;
    }

    public float dotprod(Vector3 v) {
        return x*v.x+y*v.y+z*v.z ;
    }

    @Override
    public String toString() {
        return x+", "+y+", "+z;
    }
}
