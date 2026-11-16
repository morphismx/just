package net.morphismmc.just.math;

// JAFFY: Java-side support type (no upstream counterpart) — GAP-001, GAP-006.
//
// `Option<i16>` widened into an `int` carrier: `None` is
// {@link #NONE} (`Integer.MIN_VALUE`), strictly outside every legal i16
// value (-32768..32767), so no ambiguity exists. `Option<i16>` values
// round-trip through `int` losslessly.
public final class OptI16 {
    private OptI16() {}

    /// `Option<i16>` 扩位 `int` 载体的 `None`（`Integer.MIN_VALUE`，严格安全）。
    public static final int NONE = Integer.MIN_VALUE;

    /// `Option::is_some`
    public static boolean isSome(@Opt int self) {
        return self != NONE;
    }

    /// `Option::is_none`
    public static boolean isNone(@Opt int self) {
        return self == NONE;
    }

    /// `Option::unwrap` — panics on `None` (GAP-010) and on values outside
    /// the i16 domain (a widened carrier can hold out-of-domain ints; Rust's
    /// `Option<i16>` cannot, so out-of-domain is a porting bug, not data).
    public static short unwrap(@Opt int self) {
        if (self == NONE || self != (short) self) {
            throw new IllegalStateException();
        }
        return (short) self;
    }

    /// `Option::unwrap_or` — same domain check as `unwrap` (GAP-010).
    public static short unwrapOr(@Opt int self, short def) {
        if (self == NONE) {
            return def;
        }
        if (self != (short) self) {
            throw new IllegalStateException();
        }
        return (short) self;
    }
}
