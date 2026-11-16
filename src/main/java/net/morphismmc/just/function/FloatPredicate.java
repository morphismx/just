package net.morphismmc.just.function;

// JAFFY: Java-side support type (no upstream counterpart).
// float -> boolean SAM, the primitive counterpart the JDK lacks.
@FunctionalInterface
public interface FloatPredicate {
    boolean test(float value);
}
