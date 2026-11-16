package net.morphismmc.just.function;

/// JAFFY support type (GAP-006/GAP-019): Rust `Fn(T) -> f32` closures
/// (e.g. `Rect::map` resolving `LengthPercentage` sides to f32) — JDK only
/// ships `ToDoubleFunction`, which would round-trip through double.
@FunctionalInterface
public interface ToFloatFunction<T> {
    /// `fn(T) -> f32`
    float applyAsFloat(T obj);
}
