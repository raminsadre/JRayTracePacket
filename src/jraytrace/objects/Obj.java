package jraytrace.objects;

import jraytrace.Vector3;

/**
 *
 * @author ramin
 */
public class Obj {
    public Vector3 position;
    public final String name;

    public Obj(String name, Vector3 position) {
        this.position= position;
        this.name = name;
    }
}
