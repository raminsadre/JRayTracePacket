package jraytrace;

/**
 *
 * @author ramin
 */
public class Color {
    public float r,g,b ;
    
    public Color() {
    }
    
    public Color(float r,float g,float b) {
        this.r=r ; this.g=g ; this.b=b ;
    }

    public void set(Color c) {
        this.r=c.r ; this.g=c.g ; this.b=c.b ;
    }
    
    public int getRGB() {
        return 255<<24 | (channelToInt(r)<<16) | (channelToInt(g)<<8) | channelToInt(b);
    }

    private int channelToInt(float c) {
        return Math.max(0,Math.min(255,(int)(c*255+0.5))) ;
    }
}
