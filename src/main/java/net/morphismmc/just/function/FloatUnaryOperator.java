package net.morphismmc.just.function;

// JAFFY: Java-side support type (no upstream counterpart).
// float -> float SAM, the primitive counterpart the JDK lacks (java.util.function
// only has int/long/double variants). Used by OptF32 combinators and callbacks
// where routing through `double` would break bit-exactness (GAP-008).
@FunctionalInterface
public interface FloatUnaryOperator {
    float applyAsFloat(float operand);
}
