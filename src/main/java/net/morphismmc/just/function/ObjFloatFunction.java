package net.morphismmc.just.function;

/// JAFFY support type (GAP-006): Rust `Fn(T, f32) -> f32` closures
/// (e.g. `Rect::zip_size` over a `Size<f32>`) — no JDK equivalent that keeps
/// the f32 argument unboxed.
@FunctionalInterface
public interface ObjFloatFunction<T> {
    /// `fn(T, f32) -> f32`
    float applyAsFloat(T obj, float value);
}
