package jraytrace;

import jraytrace.objects.Obj;

/**
 *
 * @author ramin
 */
public class Camera extends Obj {
    private final float fl ;
    private final Vector3 dir,right,up ;

    public Camera(String name, Vector3 position, Vector3 lookAt, Vector3 top, float fl) {
        super(name, position);

        this.fl=fl*5.0f ;
        this.dir=lookAt.subst(position).normalize() ;
        this.right=top.cross(this.dir).normalize() ;
        this.up=dir.cross(this.right).normalize() ;
    }

    public float getFL() {
        return fl;
    }

    public Vector3 getRight() {
        return right;
    }

    public Vector3 getUp() {
        return up;
    }

    public Vector3 getDir() {
        return dir;
    }
}
