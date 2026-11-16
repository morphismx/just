package net.morphismmc.just.function;

/// JAFFY support type (GAP-006): JDK `DoubleBinaryOperator` is double-based;
/// Rust `Fn(f32, f32) -> f32` closures (e.g. `Size::zip_map` on float
/// components) must not round-trip through double or boxed Float.
@FunctionalInterface
public interface FloatBinaryOperator {
    /// `fn(f32, f32) -> f32`
    float applyAsFloat(float left, float right);
}
