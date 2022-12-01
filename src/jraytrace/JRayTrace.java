package jraytrace;

import jraytrace.objects.Plane;
import jraytrace.objects.Sphere;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import jraytrace.objects.BoundingSphere;
import jraytrace.objects.RealObject;

/**
 *
 * @author ramin
 */
public class JRayTrace {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        final RenderSettings settings=new RenderSettings(800,800,2) ;
        final Material red=new Material(new Color(1,0,0),new Color(1,1,1),20,null) ;
        final Material green=new Material(new Color(0,1,0)) ;
        final Material orange=new Material(new Color(1,0.5f,0)) ;
        final Material blue=new Material(new Color(0,0,1.0f)) ;
        final Material reflective=new Material(new Color(0.1f,0.1f,0.1f),null,0,new Color(1,1,1)) ;
        
        final Camera camera=new Camera("camera",new Vector3(0,0,20),new Vector3(0,0,0),new Vector3(0,1,0),50.0f) ;
        final World world=new World(camera) ;
        
        world.setAmbientLight(new Color(0.2f,0.2f,0.2f)) ;
        world.setBackgroundColor(new Color(0.1f,0.1f,0.3f));

        world.add(new Lamp("lamp",new Vector3(10,10,10),new Color(1,1,1),true)) ;
        world.add(new Lamp("lamp",new Vector3(-10,10,10),new Color(0.2f,0.2f,0.2f),true)) ;

        world.add(new Sphere("sphere red",new Vector3(3,0,0),2,red)) ;
        final Sphere greenSphere=new Sphere("sphere green",new Vector3(-4,0,2),2,reflective) ;
        world.add(greenSphere) ;
        world.add(new Sphere("sphere shadow",new Vector3(4.5f,5,5),1,green)) ;
        world.add(new Plane("plane",new Vector3(-5,-4,-3),new Vector3(0,0,10),new Vector3(10,0,0),orange)) ;
        world.add(new Plane("plane",new Vector3(-5,-4,-3),new Vector3(10,0,0),new Vector3(0,10,0),reflective)) ;
        createBox(world,"box1",blue,new Vector3(-1,-4,1),2,2,2);
        createBox(world,"box2",red,new Vector3(-3,-4,1),1,1,1);
        createBox(world,"box3",red,new Vector3(-3,-4,3),1,1,1);
        createBox(world,"box3",red,new Vector3(-3,-4,5),1,1,1);
        createBox(world,"box4",green,new Vector3(2,-4,1),1,1,1);
        createBox(world,"box5",green,new Vector3(2,-4,3),1,1,1);
        createBox(world,"box6",green,new Vector3(2,-4,5),1,1,1);
        
        if(false) {
            // benchmarking
            final OutputDevice outputDevice=new OutputDevice();
            outputDevice.open(settings.getWidth(), settings.getHeight());
            final Renderer renderer=new Renderer(world, settings);

            // let's do some warm-up before the actual measurement
            for(int i=0;i<10;i++) {
                renderSingle(renderer, settings, outputDevice);
            }

            final long starttime=System.currentTimeMillis();
            final int numFrames=100;
            for(int i=0; i<numFrames; i++) {
                renderSingle(renderer, settings, outputDevice);
            }
            long endtime=System.currentTimeMillis();
            System.out.println("Average time per frame: "+(endtime-starttime)/numFrames+" ms");
            outputDevice.close();
        }
        else {
            final var outputDevice=new WindowOutputDevice() ;
            outputDevice.open(settings.getWidth(), settings.getHeight());
            final Renderer renderer = new Renderer(world, settings);
            renderSingle(renderer, settings, outputDevice);
            
            ((WindowOutputDevice)outputDevice).getPanel().addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    greenSphere.position = new Vector3(-(e.getX()-settings.getWidth()/2)/30.0f,
                            -(e.getY()-settings.getHeight()/2)/30.0f,
                            greenSphere.position.z);
                    renderSingle(renderer, settings, outputDevice);
                }
            });
        }
    }
    
    private static void createBox(World world,String name,Material material,Vector3 position,float xsize,float ysize,float zsize) {
        final Vector3 vx=new Vector3(xsize,0,0);
        final Vector3 vy=new Vector3(0,ysize,0);
        final Vector3 vz=new Vector3(0,0,zsize);

        final RealObject[] objects = new RealObject[]{
            new Plane(name,position.add(vy),vz,vx,material),
            new Plane(name,position,vx,vy,material),
            new Plane(name,position,vz,vy,material),
            new Plane(name,position.add(vx),vz,vy,material),
            new Plane(name,position.add(vz),vx,vy,material),
            new Plane(name,position,vz,vx,material)
        };
        
        final float boundingRadius=(float)Math.sqrt(xsize*xsize+ysize*ysize+zsize*zsize)/2;
        final Vector3 boundingPosition=position.add(new Vector3(xsize/2,ysize/2,zsize/2));
        final BoundingSphere boundingSphere=new BoundingSphere(name+"_boundingsphere",boundingPosition,boundingRadius, objects);
        world.add(boundingSphere);
    }
    
    private static void renderSingle(Renderer renderer, RenderSettings settings, OutputDevice outputDevice) {
        renderer.render(outputDevice, 0, settings.getHeight());
        outputDevice.flush();
    }

    private static void renderThreaded(RenderSettings settings, World world, OutputDevice outputDevice) {
        final int linesPerThread=50 ;
        final int numThreads=Runtime.getRuntime().availableProcessors() ;
        final ExecutorService es= Executors.newFixedThreadPool(numThreads) ;
        for(int i=0;i<settings.getHeight();i+=linesPerThread) {
            es.execute(new RenderThread(world, settings, outputDevice, i, Math.min(i+linesPerThread,settings.getHeight()))) ;
        }
        es.shutdown();
        try {
            es.awaitTermination(1, TimeUnit.DAYS);
        }
        catch (InterruptedException ex) {
        }
        outputDevice.flush();
    }    
}
