package net.morphismmc.just.math;

import net.morphismmc.just.function.FloatPredicate;
import net.morphismmc.just.function.FloatSupplier;
import net.morphismmc.just.function.FloatUnaryOperator;

// JAFFY: Java-side support type (no upstream file counterpart) — GAP-001, GAP-004, GAP-006.
// Rust `Option<f32>` stored in a `float`: `None` is the dedicated quiet-NaN payload
// `0x7FC07AFF` (registered in GAPS.md's NaN payload registry). Every other bit pattern —
// including `Float.NaN` — is `Some(x)`. Never test `isNaN` or `x != x` for `None`.
//
// Hosts scalar helpers whose Rust Self type is `Option<f32>`; method-name suffixes follow
// GAP-006 (`O` = Option<f32>, `F` = f32).
public final class OptF32 {
    private OptF32() {}

    /// Bit pattern of the `None` sentinel.
    static final int NONE_BITS = 0x7FC07AFF;

    /// The `None` value.
    public static final float NONE = Float.intBitsToFloat(NONE_BITS);

    /// `Option::is_some`
    public static boolean isSome(@Opt float self) {
        return Float.floatToRawIntBits(self) != NONE_BITS;
    }

    /// `Option::is_none`
    public static boolean isNone(@Opt float self) {
        return Float.floatToRawIntBits(self) == NONE_BITS;
    }

    /// `PartialEq` for `Option<f32>` (GAP-004): `None == None`; `Some == Some` compares with
    /// float `==`, so `Some(NaN) != Some(NaN)` and `Some(0.0) == Some(-0.0)`.
    public static boolean eq(@Opt float a, @Opt float b) {
        if (isNone(a)) {
            return isNone(b);
        }
        if (isNone(b)) {
            return false;
        }
        return a == b;
    }

    /// `Option::map`
    public static float map(@Opt float self, FloatUnaryOperator f) {
        return isNone(self) ? self : f.applyAsFloat(self);
    }

    /// `Option::map_or`
    public static float mapOr(@Opt float self, float def, FloatUnaryOperator f) {
        return isNone(self) ? def : f.applyAsFloat(self);
    }

    /// `Option::map_or_else`
    public static float mapOrElse(@Opt float self, FloatSupplier def, FloatUnaryOperator f) {
        return isNone(self) ? def.getAsFloat() : f.applyAsFloat(self);
    }

    /// `Option::inspect`
    public static float inspect(@Opt float self, FloatUnaryOperator f) {
        if (isSome(self)) {
            f.applyAsFloat(self);
        }
        return self;
    }

    /// `Option::unwrap` — GAP-010: panic maps to `IllegalStateException`; the std-lib panic
    /// message cannot be reproduced (accepted divergence), so none is faked.
    public static float unwrap(@Opt float self) {
        if (isNone(self)) {
            throw new IllegalStateException();
        }
        return self;
    }

    /// `Option::expect` — GAP-010: keeps the caller's message.
    public static float expect(@Opt float self, String msg) {
        if (isNone(self)) {
            throw new IllegalStateException(msg);
        }
        return self;
    }

    /// `Option::unwrap_or`
    public static float unwrapOr(@Opt float self, float def) {
        return isNone(self) ? def : self;
    }

    /// `Option::unwrap_or_else`
    public static float unwrapOrElse(@Opt float self, FloatSupplier f) {
        return isNone(self) ? f.getAsFloat() : self;
    }

    /// `Option::or`
    public static float or(@Opt float self, @Opt float optb) {
        return isSome(self) ? self : optb;
    }

    /// `Option::or_else`
    public static float orElse(@Opt float self, FloatSupplier f) {
        return isSome(self) ? self : f.getAsFloat();
    }

    /// `Option::and`
    public static float and(@Opt float self, @Opt float optb) {
        return isNone(self) ? self : optb;
    }

    /// `Option::and_then`
    public static float andThen(@Opt float self, FloatUnaryOperator f) {
        return isNone(self) ? self : f.applyAsFloat(self);
    }

    /// `Option::filter`
    public static float filter(@Opt float self, FloatPredicate predicate) {
        return isSome(self) && predicate.test(self) ? self : NONE;
    }

    // ---- MaybeMath impls for Self = `Option<f32>` (math.rs), suffix per GAP-006 ----

    /// `impl MaybeMath<Option<f32>, Option<f32>> for Option<f32> :: maybe_min`
    public static float maybeMinO(@Opt float self, @Opt float rhs) {
        if (isSome(self) && isSome(rhs)) {
            return F32.min(self, rhs);
        }
        return isSome(self) ? self : NONE;
    }

    /// `impl MaybeMath<Option<f32>, Option<f32>> for Option<f32> :: maybe_max`
    public static float maybeMaxO(@Opt float self, @Opt float rhs) {
        if (isSome(self) && isSome(rhs)) {
            return F32.max(self, rhs);
        }
        return isSome(self) ? self : NONE;
    }

    /// `impl MaybeMath<Option<f32>, Option<f32>> for Option<f32> :: maybe_clamp`
    public static float maybeClampO(@Opt float self, @Opt float min, @Opt float max) {
        if (isNone(self)) {
            return NONE;
        }
        if (isSome(min) && isSome(max)) {
            return F32.max(F32.min(self, max), min);
        }
        if (isNone(min) && isSome(max)) {
            return F32.min(self, max);
        }
        if (isSome(min) && isNone(max)) {
            return F32.max(self, min);
        }
        return self;
    }

    /// `impl MaybeMath<Option<f32>, Option<f32>> for Option<f32> :: maybe_add`
    public static float maybeAddO(@Opt float self, @Opt float rhs) {
        if (isSome(self) && isSome(rhs)) {
            return self + rhs;
        }
        return isSome(self) ? self : NONE;
    }

    /// `impl MaybeMath<Option<f32>, Option<f32>> for Option<f32> :: maybe_sub`
    public static float maybeSubO(@Opt float self, @Opt float rhs) {
        if (isSome(self) && isSome(rhs)) {
            return self - rhs;
        }
        return isSome(self) ? self : NONE;
    }

    /// `impl MaybeMath<f32, Option<f32>> for Option<f32> :: maybe_min`
    public static float maybeMinF(@Opt float self, float rhs) {
        return map(self, val -> F32.min(val, rhs));
    }

    /// `impl MaybeMath<f32, Option<f32>> for Option<f32> :: maybe_max`
    public static float maybeMaxF(@Opt float self, float rhs) {
        return map(self, val -> F32.max(val, rhs));
    }

    /// `impl MaybeMath<f32, Option<f32>> for Option<f32> :: maybe_clamp`
    public static float maybeClampF(@Opt float self, float min, float max) {
        return map(self, val -> F32.max(F32.min(val, max), min));
    }

    /// `impl MaybeMath<f32, Option<f32>> for Option<f32> :: maybe_add`
    public static float maybeAddF(@Opt float self, float rhs) {
        return map(self, val -> val + rhs);
    }

    /// `impl MaybeMath<f32, Option<f32>> for Option<f32> :: maybe_sub`
    public static float maybeSubF(@Opt float self, float rhs) {
        return map(self, val -> val - rhs);
    }
}
