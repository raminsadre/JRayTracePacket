package jraytrace;

import jraytrace.objects.Obj;

/**
 *
 * @author ramin
 */
public class Lamp extends Obj {
    private final Color color ;
    private final boolean hasShadow ;

    public Lamp(String name, Vector3 position, Color color, boolean hasShadow) {
        super(name,position) ;
        this.color = color;
        this.hasShadow = hasShadow;
    }

    public Color getColor() {
        return color;
    }

    public boolean hasShadow() {
        return hasShadow;
    }    
}
