package net.morphismmc.just.function;

// JAFFY: Java-side support type (no upstream counterpart).
// () -> float SAM, the primitive counterpart the JDK lacks.
@FunctionalInterface
public interface FloatSupplier {
    float getAsFloat();
}
