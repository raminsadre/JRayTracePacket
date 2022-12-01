package jraytrace;

/**
 *
 * @author ramin
 */
public class Material {
    private final Color diffuseColor ;
    private final Color specularColor ;
    private final float specularN ;
    private final Color reflectionColor ;

    public Material(Color diffuseColor) {
        this(diffuseColor,null,0,null) ;
    }
    
    public Material(Color diffuseColor, Color specularColor,float specularN, Color reflectionColor) {
        this.diffuseColor = diffuseColor;
        this.specularColor = specularColor;
        this.specularN = specularN;
        this.reflectionColor = reflectionColor;
    }    

    public Color getDiffuseColor() {
        return diffuseColor;
    }
    
    public Color getSpecularColor() {
        return specularColor;
    }

    public float getSpecularN() {
        return specularN;
    }

    public Color getReflectionColor() {
        return reflectionColor;
    }    
}
