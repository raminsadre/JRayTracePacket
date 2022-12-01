package jraytrace;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;

public class SIMD {
    public static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_256;
    
    public static final float EPSILON = 1e-3f;
    
    public static final FloatVector ZERO = FloatVector.zero(SPECIES);

    public static FloatVector create(float value) {
        return FloatVector.broadcast(SPECIES,value);
    }

    public static FloatVector fromArray(float[] array, int offset) {
        return FloatVector.fromArray(SPECIES, array, offset);
    }
}
