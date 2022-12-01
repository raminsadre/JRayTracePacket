package jraytrace;

import java.util.List;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jraytrace.objects.RealObject;

/**
 *
 * @author ramin
 */
public class Renderer {
    private final World world ;
    private final RenderSettings settings ;

    public Renderer(World world,RenderSettings settings) {
        this.world = world;
        this.settings = settings;
    }
    
    public void render(OutputDevice outputDevice,int startLine,int endLine) {
        final int width=settings.getWidth() ;
        final int height=settings.getHeight() ;
        final Camera camera=world.getCamera() ;

        final Vector3 dx=camera.getRight().mult(250.0f/(camera.getFL()*width)) ;
        final Vector3 dy=camera.getUp().mult(-250.0f/(camera.getFL()*height)) ;

        final RayPacket rays = new RayPacket(camera.position);
        final Color[] colors = new Color[RayPacket.SIZE];
        final HitInfoPacket hitInfo = new HitInfoPacket();

        for(int y=startLine;y<endLine;y++) {
            final Vector3 rayDirForThisLine=camera.getDir().add(dy,y-height/2) ;
            for(int x=0;x<width;x+=RayPacket.SIZE) {
                // prepare ther ray packet
                for(int i=0;i<RayPacket.SIZE;i++) {
                    rays.setDirection(i, rayDirForThisLine.add(dx,x+i-width/2).normalize());
                }
                // trace the rays
                traceRay(rays,0, hitInfo, colors) ;
                // draw the result
                for(int i=0;i<colors.length;i++)
                    outputDevice.setPixel(x+i, y, colors[i]);
            }
        }
    }

    private void traceRay(RayPacket rays, int depth, HitInfoPacket hitInfo, Color[] colors) {
        // initialize the hit info
        hitInfo.hitMask = 0;
        for(int i=0;i<RayPacket.SIZE;i++) {
            hitInfo.distance[i] = Float.MAX_VALUE;
        }

        // search for closest intersections
        for(RealObject obj : world.getRealObjects()) {
            obj.findCollision(rays, hitInfo) ;
        }
        
        // calculate the ambient colors
        for(int i=0; i<RayPacket.SIZE; i++) {
            colors[i] = hitInfo.isHit(i)
                    ? computeAmbientLight(hitInfo.object[i])
                    : world.getBackgroundColor();
        }
        
        // calculate the color from lights
        // of course, we only need to do this if there is at least one ray hitting an object
        if(hitInfo.hitMask!=0) {
            computeLampLight(colors, rays, hitInfo);
        }
        
        // todo
        computeReflection(colors, rays,hitInfo,depth) ;
    }

    // This method is used to just quickly check whether there is any object
    // in the path of the ray, without being interested in any more information.
    // For each ray, you can specify an object that is ignored by the check.
    private static VectorMask<Float> isCollision(RayPacket rays, List<RealObject> objectsToCheck, long rayMask) {
        // ignore inactive rays in the search
        VectorMask<Float> result = VectorMask.fromLong(SIMD.SPECIES, ~rayMask);
          
        for(RealObject obj : objectsToCheck) {          
            result = result.or(obj.isCollision(rays));
            // all rays have reported a hit? -> stop searching
            if(result.allTrue()) {
                break;
            }
        }
        return result;
    }

    private Color computeAmbientLight(RealObject object) {
        final Color ambientColor = world.getAmbientColor() ;
        final Color diffuseColor = object.getMaterial().getDiffuseColor();
        return new Color(ambientColor.r*diffuseColor.r, ambientColor.g*diffuseColor.g, ambientColor.b*diffuseColor.b);
    }
    
    private void computeLampLight(Color[] colors, RayPacket viewRays, HitInfoPacket hitInfo) {       
        // check whether we need to do specular reflection calculations for any of the rays
        boolean hasSpecularReflection = false;
        for(int i=0;i<RayPacket.SIZE;i++) {
            if(hitInfo.isHit(i)) {
                hasSpecularReflection |= hitInfo.object[i].getMaterial().getSpecularColor()!=null;
            }
        }
        
        FloatVector hitLocationX = SIMD.fromArray(hitInfo.location, 0);
        FloatVector hitLocationY = SIMD.fromArray(hitInfo.location, RayPacket.SIZE);
        FloatVector hitLocationZ = SIMD.fromArray(hitInfo.location, RayPacket.SIZE*2);

        FloatVector hitNormalX = SIMD.fromArray(hitInfo.normal, 0);
        FloatVector hitNormalY = SIMD.fromArray(hitInfo.normal, RayPacket.SIZE);
        FloatVector hitNormalZ = SIMD.fromArray(hitInfo.normal, RayPacket.SIZE*2);
        
        for(Lamp lamp : world.getLamps()) {           
            // direction to lamp = lampPosition - hitLocation
            FloatVector lightDirX = SIMD.create(lamp.position.x).sub(hitLocationX);
            FloatVector lightDirY = SIMD.create(lamp.position.y).sub(hitLocationY);
            FloatVector lightDirZ = SIMD.create(lamp.position.z).sub(hitLocationZ);
            // normalize the direction
            FloatVector lightDirLen = lightDirX.fma(lightDirX, lightDirY.fma(lightDirY, lightDirZ.mul(lightDirZ))).sqrt();
            lightDirX = lightDirX.div(lightDirLen);
            lightDirY = lightDirY.div(lightDirLen);
            lightDirZ = lightDirZ.div(lightDirLen);
            
            // cosinus of angle between lamp direction and surface normal
            FloatVector NdotL = lightDirX.fma(hitNormalX, lightDirY.fma(hitNormalY, lightDirZ.mul(hitNormalZ)));
            // if the cosinus is negative, the light is behind the surface
            VectorMask<Float> notLightMask = NdotL.test(VectorOperators.IS_NEGATIVE);
            if(notLightMask.allTrue()) {
                continue;
            }
            
            // do we want shadow for this light?
            // if yes, we have to check whether there is any object between
            // us and the light.
            if(lamp.hasShadow()) {
                RayPacket lightRays = new RayPacket(hitInfo.location);
                lightDirX.intoArray(lightRays.dirs, 0);
                lightDirY.intoArray(lightRays.dirs, RayPacket.SIZE);
                lightDirZ.intoArray(lightRays.dirs, RayPacket.SIZE*2);
                notLightMask = notLightMask.or(isCollision(lightRays, world.getRealObjects(), hitInfo.hitMask));
            }
            
            // calculate the half vector between view dir and light dir.
            // this is only needed if there are objects with specular reflection
            FloatVector NdotH = null;
            if(hasSpecularReflection) {
                // h = lightDir - viewDir
                FloatVector hX = lightDirX.sub(viewRays.dirX());
                FloatVector hY = lightDirY.sub(viewRays.dirY());
                FloatVector hZ = lightDirZ.sub(viewRays.dirZ());
                // normalize h
                FloatVector hLen = hX.fma(hX, hY.fma(hY, hZ.mul(hZ))).sqrt();
                hX = hX.div(hLen);
                hY = hY.div(hLen);
                hZ = hZ.div(hLen);
                // NdotH = surfaceNormal _dotprod_ h
                NdotH = hitNormalX.fma(hX, hitNormalY.fma(hY, hitNormalZ.mul(hZ)));
            }
            
            VectorMask<Float> lightMask = notLightMask.not();
            for(int i=0;i<RayPacket.SIZE;i++) {
                if(hitInfo.isHit(i) && lightMask.laneIsSet(i)) {
                    // compute diffuse color
                    Color c = colors[i];
                    final Color diffuseColor = hitInfo.object[i].getMaterial().getDiffuseColor() ;
                    final Color lampColor = lamp.getColor() ;
                    final float diffFactor = (float)NdotL.lane(i) ;  // no attenuation at the moment
                    c.r += lampColor.r*diffuseColor.r*diffFactor ;
                    c.g += lampColor.g*diffuseColor.g*diffFactor ;
                    c.b += lampColor.b*diffuseColor.b*diffFactor ;

                    // compute specular color
                    final Color specularColor=hitInfo.object[i].getMaterial().getSpecularColor() ;
                    if(specularColor!=null) {
                        float ndoth = NdotH.lane(i);
                        if(ndoth>0.0) {
                            final float specN=hitInfo.object[i].getMaterial().getSpecularN() ;
                            final float specFactor=(float)Math.pow(ndoth,specN) ;  // no attenuation at the moment
                            c.r += lampColor.r*specularColor.r*specFactor ;
                            c.g += lampColor.g*specularColor.g*specFactor ;
                            c.b += lampColor.b*specularColor.b*specFactor ;
                        }
                    }
                }
            }            
        }
    }

    // Warning! Slow. No SIMD acceleration implemented.
    // This method casts single rays by creating ray packets with identical rays.
    // This is actually slower than not using ray packets at all.
    private void computeReflection(Color[] color, RayPacket rays, HitInfoPacket hitInfo,int depth) {
        Color[] receivedColor = new Color[RayPacket.SIZE]; 
        
        // stop recursion?
        if(depth>settings.getMaxDepth()) {
            final Color bg = world.getBackgroundColor();
            for(int i=0;i<RayPacket.SIZE;i++) {
                receivedColor[i] = bg;
            }
        }
        else {        
            // check whether we need to do reflection calculations for any of the rays
            boolean hasReflection = false;
            for(int i=0;i<RayPacket.SIZE;i++) {
                if(hitInfo.isHit(i)) {
                    hasReflection |= hitInfo.object[i].getMaterial().getReflectionColor()!=null;
                }
            }
            if(!hasReflection) {
                return;
            }

            FloatVector hitNormalX = SIMD.fromArray(hitInfo.normal, 0);
            FloatVector hitNormalY = SIMD.fromArray(hitInfo.normal, RayPacket.SIZE);
            FloatVector hitNormalZ = SIMD.fromArray(hitInfo.normal, RayPacket.SIZE*2);

            FloatVector rayDirX = rays.dirX();
            FloatVector rayDirY = rays.dirY();
            FloatVector rayDirZ = rays.dirZ();

            // dp = -2 * hitNormal _dotprod_ rayDir
            FloatVector dp = hitNormalX.fma(rayDirX, hitNormalY.fma(rayDirY, hitNormalZ.mul(rayDirZ))).mul(-2.0f);
            // reflected rayDir = rayDir + hitNormal * dp
            FloatVector reflDirX = hitNormalX.fma(dp, rayDirX);
            FloatVector reflDirY = hitNormalY.fma(dp, rayDirY);
            FloatVector reflDirZ = hitNormalZ.fma(dp, rayDirZ);

            RayPacket reflectedRays = new RayPacket(hitInfo.location);
            reflDirX.intoArray(reflectedRays.dirs,0);
            reflDirY.intoArray(reflectedRays.dirs,RayPacket.SIZE);
            reflDirZ.intoArray(reflectedRays.dirs,RayPacket.SIZE*2);


            HitInfoPacket reflectedHitInfos = new HitInfoPacket();

            traceRay(reflectedRays, depth+1, reflectedHitInfos, receivedColor);
        }
        
        // calculate color
        for(int i=0;i<RayPacket.SIZE;i++) {
            if(hitInfo.isHit(i)) {
                final Color reflectionColor = hitInfo.object[i].getMaterial().getReflectionColor();
                if(reflectionColor!=null) {
                    color[i].r += receivedColor[i].r * reflectionColor.r ;
                    color[i].g += receivedColor[i].g * reflectionColor.g ;
                    color[i].b += receivedColor[i].b * reflectionColor.b ;
                }
            }
        }
    }
}
