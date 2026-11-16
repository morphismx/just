package net.morphismmc.just.math;

// JAFFY: Java-side support type (no upstream file counterpart) — GAP-002, GAP-003, GAP-006.
// Hosts scalar helpers whose Rust Self type is `f32`. When several same-named impls exist
// for different trait type parameters, the method name takes a suffix per GAP-006
// (`O` = Option<f32>, `F` = f32).
//
// NaN handling measured against rustc 1.96.0 (x86_64): `f32::max/min` return the non-NaN
// operand, return the *second* operand when both are NaN, and always pick +0.0 for max /
// -0.0 for min on ±0 ties — identical to `Math.max/min` once NaN is filtered out.
public final class F32 {
    private F32() {}

    /// Rust `f32::max` (GAP-002).
    public static float max(float a, float b) {
        if (Float.isNaN(a)) {
            return b;
        }
        if (Float.isNaN(b)) {
            return a;
        }
        return Math.max(a, b);
    }

    /// Rust `f32::min` (GAP-002).
    public static float min(float a, float b) {
        if (Float.isNaN(a)) {
            return b;
        }
        if (Float.isNaN(b)) {
            return a;
        }
        return Math.min(a, b);
    }

    /// Rust `f32::round`: nearest integer, ties away from zero (GAP-003).
    /// `Math.round` rounds towards positive infinity and `Math.rint` uses banker's
    /// rounding, so neither is usable.
    public static float round(float value) {
        return Math.copySign((float) Math.floor(Math.abs(value) + 0.5), value);
    }

    /// Rust `f32::ceil`. The double round-trip is exact (GAP-008): every `float` is
    /// exactly representable as `double`, and `ceil` of it is a `float`-exact integer.
    public static float ceil(float value) {
        return (float) Math.ceil(value);
    }

    /// Rust `f32::floor`. Exact via double for the same reason as {@link #ceil} (GAP-008).
    public static float floor(float value) {
        return (float) Math.floor(value);
    }

    /// Rust `f32::abs`.
    public static float abs(float value) {
        return Math.abs(value);
    }

    /// Rust `f32::clamp`: panics when `min > max` or either bound is NaN (GAP-010).
    public static float clamp(float value, float min, float max) {
        if (!(min <= max) || Float.isNaN(min) || Float.isNaN(max)) {
            throw new IllegalStateException("min > max, or either was NaN");
        }
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    // ---- MaybeMath impls for Self = `f32` (math.rs), suffix per GAP-006 ----

    /// `impl MaybeMath<Option<f32>, f32> for f32 :: maybe_min`
    public static float maybeMinO(float self, @Opt float rhs) {
        return OptF32.isSome(rhs) ? min(self, rhs) : self;
    }

    /// `impl MaybeMath<Option<f32>, f32> for f32 :: maybe_max`
    public static float maybeMaxO(float self, @Opt float rhs) {
        return OptF32.isSome(rhs) ? max(self, rhs) : self;
    }

    /// `impl MaybeMath<Option<f32>, f32> for f32 :: maybe_clamp`
    public static float maybeClampO(float self, @Opt float min, @Opt float max) {
        if (OptF32.isSome(min) && OptF32.isSome(max)) {
            return max(min(self, max), min);
        }
        if (OptF32.isNone(min) && OptF32.isSome(max)) {
            return min(self, max);
        }
        if (OptF32.isSome(min) && OptF32.isNone(max)) {
            return max(self, min);
        }
        return self;
    }

    /// `impl MaybeMath<Option<f32>, f32> for f32 :: maybe_add`
    public static float maybeAddO(float self, @Opt float rhs) {
        return OptF32.isSome(rhs) ? self + rhs : self;
    }

    /// `impl MaybeMath<Option<f32>, f32> for f32 :: maybe_sub`
    public static float maybeSubO(float self, @Opt float rhs) {
        return OptF32.isSome(rhs) ? self - rhs : self;
    }
}
