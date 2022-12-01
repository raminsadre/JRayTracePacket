package jraytrace;

import jraytrace.objects.RealObject;

import java.util.ArrayList;

/**
 *
 * @author ramin
 */
public class World {
    private final Camera camera ;
    private final ArrayList<Lamp> lamps=new ArrayList<>() ;
    private final ArrayList<RealObject> realObjects=new ArrayList<>() ;
    private Color backgroundColor=new Color(0,0,0) ;
    private Color ambientColor=new Color(0,0,0) ;

    // creates an empty world without ambient light and black background
    public World(Camera camera) {
        this.camera = camera;
    }
    
    public Camera getCamera() {
        return camera;
    }      

    public Color getBackgroundColor() {
        return backgroundColor ;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor=backgroundColor ;
    }
    
    public Color getAmbientColor() {
        return ambientColor;
    }    
    
    public void setAmbientLight(Color ambientColor) {
        this.ambientColor=ambientColor ;
    }
    
    public void add(Lamp lamp) {
        lamps.add(lamp) ;
    }
    
    public void add(RealObject object) {
        realObjects.add(object) ;
    }

    public ArrayList<Lamp> getLamps() {
        return lamps ;
    }    
    
    public ArrayList<RealObject> getRealObjects() {
        return realObjects ;
    }
}
