package jraytrace;

/**
 *
 * @author ramin
 */
public class RenderSettings {
    private final int width ;
    private final int height ;
    private final int maxDepth ;

    public RenderSettings(int width, int height,int maxDepth) {
        this.width = width;
        this.height = height;
        this.maxDepth = maxDepth ;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getMaxDepth() {
        return maxDepth;
    }
}
