package jraytrace;

/**
 *
 * @author ramin
 */
public class RenderThread implements Runnable {
    private final Renderer renderer ;
    private final OutputDevice outputDevice ;
    private final int startLine,endLine ;
    
    public RenderThread(World world,RenderSettings settings,OutputDevice outputDevice,int startLine,int endLine) {
        this.renderer=new Renderer(world, settings);
        this.outputDevice=outputDevice ;
        this.startLine=startLine;
        this.endLine=endLine ;
    }

    @Override
    public void run() {
        renderer.render(outputDevice, startLine, endLine);
    }
}
