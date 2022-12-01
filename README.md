# A simple real-time ray tracer with SIMD in Java (no GPU)

This project implements a backward raytracer with coherent rays in Java by making extensive use
of the SIMD support from the incubator vector module.

This requires Java 16 or later. Compile with the option
```
--add-modules jdk.incubator.vector
```
and run with
```
--enable-preview --add-modules jdk.incubator.vector
```
You will not need those options once the vector module leaves the incubator stange.

# Remarks on the implementation

By default, the rendering runs in a single thread but there is also a working multi-threaded implementation
(replace the calls of the method renderSingle by renderThreaded in the main class JRayTrace).

The ray tracer doesn't use any advanced space partitioning, only bounding spheres to accelerate the detection
of hits with the cubes. I told you it's a simple ray tracer, didn't I?

There are some bugs: Note the wrong shadow on the red sphere and
the ragged edges of the cubes. Those were also present in my original non-SIMD code, which means it's a math
problem and not related to the SIMD module :)
