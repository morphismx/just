package net.morphismmc.just.math;

// JAFFY: Java-side support type (no upstream counterpart) — GAP-001, GAP-006.
//
// `Option<usize>`/`Option<u16>`/`Option<u8>` stored in an `int`:
// `None` is {@link #NONE} (-1). Safety per carrier:
//   u16/u8   — -1 is strictly outside the domain: safe.
//   u32      — -1 == u32::MAX (0xFFFFFFFF): `Some(u32::MAX)` would read as
//              None — NOT safe for full-range u32. Only use where values are
//              bounded below 0x80000000 (indexes/counts).
//   usize    — Java int carrier already narrows usize per GAP-014; index
//              semantics keep values << 2^31, so -1 is unreachable.
//   i32      — -1 is a legal Some(-1): NOT safe. Keep `@Nullable Integer`.
public final class OptI32 {
    private OptI32() {}

    /// `Option<usize>`/`Option<u16>`/`Option<u8>` 等索引/计数载体的 `None`。
    public static final int NONE = -1;

    /// `Option::is_some`
    public static boolean isSome(@Opt int self) {
        return self != NONE;
    }

    /// `Option::is_none`
    public static boolean isNone(@Opt int self) {
        return self == NONE;
    }

    /// `Option::unwrap` — panics on `None` (GAP-010).
    public static int unwrap(@Opt int self) {
        if (self == NONE) {
            throw new IllegalStateException();
        }
        return self;
    }

    /// `Option::unwrap_or`
    public static int unwrapOr(@Opt int self, int def) {
        return self != NONE ? self : def;
    }

    /// `Option::map` — f maps `int -> int`.
    public static int map(@Opt int self, java.util.function.IntUnaryOperator f) {
        return self != NONE ? f.applyAsInt(self) : NONE;
    }
}
