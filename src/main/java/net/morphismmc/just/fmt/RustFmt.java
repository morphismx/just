package net.morphismmc.just.fmt;

// JAFFY: Rust `f32`/`f64` Display + Debug formatting (GAP-016).
// Glue layer ported from rustc 1.96.0 core::fmt::float; digit generation is
// the same Grisu3 + Dragon4 fallback std uses (see Grisu.java/Dragon.java).
// Output must be byte-identical to Rust — `Float.toString` etc. are banned.
//
// Only the forms taffy can observe are provided: `{}` (Display) and `{:?}`
// (Debug) without precision/sign-plus/padding. LowerExp/UpperExp and the
// exact/fixed modes ({:.N}) have no upstream call sites and are not ported.
public final class RustFmt {
    private RustFmt() {}

    /// Rust `{}` for `f32` — `float_to_decimal_display` with `Sign::Minus`,
    /// no precision → `to_shortest_str(..., min_precision = 0)`.
    public static String display(float v) {
        byte[] buf = new byte[Flt2dec.MAX_SIG_DIGITS];
        Flt2dec.Part[] parts = new Flt2dec.Part[6];
        Flt2dec.DecodeResult dr = Flt2dec.decodeF32(v);
        Flt2dec.Formatted f = Flt2dec.toShortestStr(
                dr.negative(), dr.decoded(), Flt2dec.Sign.MINUS, 0, buf, parts);
        return assemble(f);
    }

    /// Rust `{}` for `f64`.
    public static String display(double v) {
        byte[] buf = new byte[Flt2dec.MAX_SIG_DIGITS];
        Flt2dec.Part[] parts = new Flt2dec.Part[6];
        Flt2dec.DecodeResult dr = Flt2dec.decodeF64(v);
        Flt2dec.Formatted f = Flt2dec.toShortestStr(
                dr.negative(), dr.decoded(), Flt2dec.Sign.MINUS, 0, buf, parts);
        return assemble(f);
    }

    /// Rust `{:?}` for `f32` — `float_to_general_debug` with `Sign::Minus`,
    /// no precision: exponential when `abs != 0 && abs < 1e-4 || abs >= 1e16`,
    /// else `to_shortest_str(..., min_precision = 1)`.
    public static String debug(float v) {
        byte[] buf = new byte[Flt2dec.MAX_SIG_DIGITS];
        Flt2dec.Part[] parts = new Flt2dec.Part[6];
        Flt2dec.DecodeResult dr = Flt2dec.decodeF32(v);
        Flt2dec.Formatted f;
        if (alreadyRoundedShouldUseExp(v)) {
            // float_to_exponential_common_shortest, dec_bounds = (0, 0), upper = false
            f = Flt2dec.toShortestExpStr(
                    dr.negative(), dr.decoded(), Flt2dec.Sign.MINUS, 0, 0, false, buf, parts);
        } else {
            f = Flt2dec.toShortestStr(
                    dr.negative(), dr.decoded(), Flt2dec.Sign.MINUS, 1, buf, parts);
        }
        return assemble(f);
    }

    /// Rust `{:?}` for `f64`.
    public static String debug(double v) {
        byte[] buf = new byte[Flt2dec.MAX_SIG_DIGITS];
        Flt2dec.Part[] parts = new Flt2dec.Part[6];
        Flt2dec.DecodeResult dr = Flt2dec.decodeF64(v);
        Flt2dec.Formatted f;
        if (alreadyRoundedShouldUseExp(v)) {
            f = Flt2dec.toShortestExpStr(
                    dr.negative(), dr.decoded(), Flt2dec.Sign.MINUS, 0, 0, false, buf, parts);
        } else {
            f = Flt2dec.toShortestStr(
                    dr.negative(), dr.decoded(), Flt2dec.Sign.MINUS, 1, buf, parts);
        }
        return assemble(f);
    }

    /// `already_rounded_value_should_use_exponential` (f32).
    private static boolean alreadyRoundedShouldUseExp(float v) {
        float abs = Math.abs(v); // f32::abs — NaN -> NaN -> both comparisons false
        return (abs != 0.0f && abs < 1e-4f) || abs >= 1e16f;
    }

    /// `already_rounded_value_should_use_exponential` (f64).
    private static boolean alreadyRoundedShouldUseExp(double v) {
        double abs = Math.abs(v);
        return (abs != 0.0 && abs < 1e-4) || abs >= 1e16;
    }

    /// `Formatted::write` into a String. All parts are ASCII.
    /// Rust `{:?}` for `NodeId` — the derive output `NodeId(<u64>)`
    /// (NodeId is a bare `long` per GAP-015).
    public static String nodeId(long v) {
        return "NodeId(" + Long.toUnsignedString(v) + ")";
    }

    private static String assemble(Flt2dec.Formatted f) {
        StringBuilder sb = new StringBuilder(24);
        sb.append(f.sign());
        for (int i = 0; i < f.len(); i++) {
            writePart(sb, f.parts()[i]);
        }
        return sb.toString();
    }

    /// `Part::write`.
    private static void writePart(StringBuilder sb, Flt2dec.Part part) {
        switch (part) {
            case Flt2dec.Part.Zero z -> {
                for (int i = 0; i < z.n(); i++) {
                    sb.append('0');
                }
            }
            case Flt2dec.Part.Num num -> {
                // u16 digits, most significant first — same as Integer.toString
                // for non-negative values.
                sb.append(Integer.toString(num.v()));
            }
            case Flt2dec.Part.Copy c -> {
                for (int i = c.off(); i < c.off() + c.len(); i++) {
                    sb.append((char) (c.buf()[i] & 0xFF));
                }
            }
        }
    }
}
