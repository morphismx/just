package net.morphismmc.just.fmt;

import java.util.Arrays;

// JAFFY: port of rustc 1.96.0 core::num::imp::flt2dec (shortest-mode subset)
// plus core::num::imp::flt2dec::decoder, core::num::imp::flt2dec::estimator and
// core::num::imp::fmt (Part/Formatted).
//.
//
// Scope: only what Rust `Display`/`Debug` for f32/f64 needs — the shortest mode
// (`to_shortest_str`, `to_shortest_exp_str`, `digits_to_dec_str`,
// `digits_to_exp_str`, `round_up`, `decode`, `estimate_scaling_factor`).
// The exact/fixed modes (`format_exact`, `to_exact_*`) are not ported: no Rust
// `{}`/`{:?}` site in taffy uses precision (GAP-016).
//
// Rust `u64`/`u32` -> Java long/int with unsigned semantics where required
// (GAP-014): `Long/Integer.compareUnsigned`, `divideUnsigned`,
// `remainderUnsigned`, `>>>` shifts.
final class Flt2dec {
    private Flt2dec() {}

    /// `MAX_SIG_DIGITS` — minimum buffer size for the shortest mode.
    static final int MAX_SIG_DIGITS = 17;

    /// Result of a digit-generation strategy: digits are in buf[0..len].
    record FormatResult(int len, int exp) {}

    /// `Decoded` — decoded unsigned finite value `mant * 2^exp` with error range.
    record Decoded(long mant, long minus, long plus, int exp, boolean inclusive) {}

    /// `FullDecoded`.
    sealed interface FullDecoded {
        record Nan() implements FullDecoded {}
        record Infinite() implements FullDecoded {}
        record Zero() implements FullDecoded {}
        record Finite(Decoded d) implements FullDecoded {}
    }

    /// `(negative, FullDecoded)` as returned by `decode`.
    record DecodeResult(boolean negative, FullDecoded decoded) {}

    /// `Sign` formatting option.
    enum Sign {
        MINUS,
        MINUS_PLUS,
    }

    /// `Part` — formatted parts.
    sealed interface Part {
        /// Given number of zero digits.
        record Zero(int n) implements Part {}
        /// A literal number up to 5 digits (u16).
        record Num(int v) implements Part {}
        /// A verbatim copy of given bytes.
        record Copy(byte[] buf, int off, int len) implements Part {}
    }

    /// `Formatted` — sign plus parts.
    record Formatted(String sign, Part[] parts, int len) {}

    // Literal byte strings used by Part::Copy sites.
    static final byte[] B_NAN = ascii("NaN");
    static final byte[] B_INF = ascii("inf");
    static final byte[] B_0 = ascii("0");
    static final byte[] B_0DOT = ascii("0.");
    static final byte[] B_DOT = ascii(".");
    static final byte[] B_E = ascii("e");
    static final byte[] B_EU = ascii("E");
    static final byte[] B_E_MINUS = ascii("e-");
    static final byte[] B_EU_MINUS = ascii("E-");
    static final byte[] B_0E0 = ascii("0e0");
    static final byte[] B_0EU0 = ascii("0E0");

    private static byte[] ascii(String s) {
        byte[] b = new byte[s.length()];
        for (int i = 0; i < s.length(); i++) {
            b[i] = (byte) s.charAt(i);
        }
        return b;
    }

    // --------------------------------------------------------------------
    // decoder.rs — decode + integer_decode for f32/f64.
    // --------------------------------------------------------------------

    /// `f32::integer_decode` + `decode`. `minnorm.0` for f32 is `1 << 23`.
    static DecodeResult decodeF32(float v) {
        int bits = Float.floatToRawIntBits(v);
        boolean negative = (bits >>> 31) != 0;
        int expField = (bits & 0x7F800000) >>> 23;
        int rawMant = bits & 0x007FFFFF;
        if (expField == 0xFF) {
            return new DecodeResult(negative, rawMant != 0 ? new FullDecoded.Nan() : new FullDecoded.Infinite());
        }
        if (expField == 0 && rawMant == 0) {
            return new DecodeResult(negative, new FullDecoded.Zero());
        }
        long mant = expField == 0 ? ((long) rawMant) << 1 : (rawMant | 0x800000L);
        int exp = expField - 150; // EXP_BIAS(127) + SIG_BITS(23)
        boolean even = (mant & 1) == 0;
        Decoded d;
        if (expField == 0) {
            // Subnormal: (mant - 2, exp) -- (mant, exp) -- (mant + 2, exp)
            d = new Decoded(mant, 1, 1, exp, even);
        } else if (mant == 0x800000L) {
            // minnorm: (maxmant, exp - 1) -- (minnormmant, exp) -- (minnormmant + 1, exp)
            d = new Decoded(mant << 2, 1, 2, exp - 2, even);
        } else {
            // (mant - 1, exp) -- (mant, exp) -- (mant + 1, exp)
            d = new Decoded(mant << 1, 1, 1, exp - 1, even);
        }
        return new DecodeResult(negative, new FullDecoded.Finite(d));
    }

    /// `f64::integer_decode` + `decode`. `minnorm.0` for f64 is `1 << 52`.
    static DecodeResult decodeF64(double v) {
        long bits = Double.doubleToRawLongBits(v);
        boolean negative = (bits >>> 63) != 0;
        int expField = (int) ((bits & 0x7FF0000000000000L) >>> 52);
        long rawMant = bits & 0x000FFFFFFFFFFFFFL;
        if (expField == 0x7FF) {
            return new DecodeResult(negative, rawMant != 0 ? new FullDecoded.Nan() : new FullDecoded.Infinite());
        }
        if (expField == 0 && rawMant == 0) {
            return new DecodeResult(negative, new FullDecoded.Zero());
        }
        long mant = expField == 0 ? rawMant << 1 : (rawMant | 0x0010000000000000L);
        int exp = expField - 1075; // EXP_BIAS(1023) + SIG_BITS(52)
        boolean even = (mant & 1) == 0;
        Decoded d;
        if (expField == 0) {
            d = new Decoded(mant, 1, 1, exp, even);
        } else if (mant == 0x0010000000000000L) {
            d = new Decoded(mant << 2, 1, 2, exp - 2, even);
        } else {
            d = new Decoded(mant << 1, 1, 1, exp - 1, even);
        }
        return new DecodeResult(negative, new FullDecoded.Finite(d));
    }

    // --------------------------------------------------------------------
    // estimator.rs — estimate_scaling_factor
    // --------------------------------------------------------------------

    /// Finds `k_0` such that `10^(k_0-1) < mant * 2^exp <= 10^(k_0+1)`.
    static int estimateScalingFactor(long mant, int exp) {
        long nbits = 64 - Long.numberOfLeadingZeros(mant - 1);
        // 1292913986 = floor(2^32 * log_10 2)
        return (short) (((nbits + exp) * 1292913986L) >> 32); // JAFFY(GAP-014): `as i16`
    }

    // --------------------------------------------------------------------
    // mod.rs — round_up
    // --------------------------------------------------------------------

    /// `round_up(d) -> Option<u8>`; returns the new digit (as int) or -1 for `None`.
    /// Mutates `d[0..len]` in place.
    static int roundUp(byte[] d, int len) {
        for (int i = len - 1; i >= 0; i--) {
            if (d[i] != '9') {
                // d[i+1..len] is all nines
                d[i] += 1;
                Arrays.fill(d, i + 1, len, (byte) '0');
                return -1;
            }
        }
        if (len == 0) {
            // an empty buffer rounds up (a bit strange but reasonable)
            return '1';
        }
        // 999..999 rounds to 1000..000 with an increased exponent
        d[0] = '1';
        Arrays.fill(d, 1, len, (byte) '0');
        return '0';
    }

    // --------------------------------------------------------------------
    // mod.rs — digits_to_dec_str / digits_to_exp_str
    // --------------------------------------------------------------------

    /// `digits_to_dec_str` — returns the number of parts written.
    static int digitsToDecStr(byte[] buf, int len, int exp, int fracDigits, Part[] parts) {
        if (exp <= 0) {
            // [0.][000...000][1234][____]
            int minusExp = -exp;
            parts[0] = new Part.Copy(B_0DOT, 0, B_0DOT.length);
            parts[1] = new Part.Zero(minusExp);
            parts[2] = new Part.Copy(buf, 0, len);
            if (fracDigits > len && fracDigits - len > minusExp) {
                parts[3] = new Part.Zero((fracDigits - len) - minusExp);
                return 4;
            }
            return 3;
        } else if (exp < len) {
            // [12][.][34][____]
            parts[0] = new Part.Copy(buf, 0, exp);
            parts[1] = new Part.Copy(B_DOT, 0, B_DOT.length);
            parts[2] = new Part.Copy(buf, exp, len - exp);
            if (fracDigits > len - exp) {
                parts[3] = new Part.Zero(fracDigits - (len - exp));
                return 4;
            }
            return 3;
        } else {
            // [1234][____0000] or [1234][__][.][__]
            parts[0] = new Part.Copy(buf, 0, len);
            parts[1] = new Part.Zero(exp - len);
            if (fracDigits > 0) {
                parts[2] = new Part.Copy(B_DOT, 0, B_DOT.length);
                parts[3] = new Part.Zero(fracDigits);
                return 4;
            }
            return 2;
        }
    }

    /// `digits_to_exp_str` — returns the number of parts written.
    static int digitsToExpStr(byte[] buf, int len, int exp, int fracDigits, boolean upper, Part[] parts) {
        int n = 0;

        parts[n++] = new Part.Copy(buf, 0, 1);

        // The first generated digit becomes the integral digit.
        int actualFracDigits = len - 1;
        if (actualFracDigits > 0 || fracDigits > 0) {
            parts[n++] = new Part.Copy(B_DOT, 0, B_DOT.length);
            if (actualFracDigits > 0) {
                parts[n++] = new Part.Copy(buf, 1, len - 1);
            }
            if (fracDigits > actualFracDigits) {
                parts[n++] = new Part.Zero(fracDigits - actualFracDigits);
            }
        }

        // 0.1234 x 10^exp = 1.234 x 10^(exp-1)
        int e = exp - 1;
        if (e < 0) {
            parts[n] = new Part.Copy(upper ? B_EU_MINUS : B_E_MINUS, 0, 2);
            parts[n + 1] = new Part.Num(-e & 0xFFFF); // `-exp as u16`
        } else {
            parts[n] = new Part.Copy(upper ? B_EU : B_E, 0, 1);
            parts[n + 1] = new Part.Num(e & 0xFFFF); // `exp as u16`
        }
        return n + 2;
    }

    // --------------------------------------------------------------------
    // mod.rs — determine_sign
    // --------------------------------------------------------------------

    static String determineSign(Sign sign, FullDecoded decoded, boolean negative) {
        if (decoded instanceof FullDecoded.Nan) {
            return "";
        }
        return switch (sign) {
            case MINUS -> negative ? "-" : "";
            case MINUS_PLUS -> negative ? "-" : "+";
        };
    }

    // --------------------------------------------------------------------
    // mod.rs — to_shortest_str / to_shortest_exp_str
    //
    // `format_shortest` is hardcoded to `Grisu.formatShortest`, the only
    // algorithm Rust uses here.
    // --------------------------------------------------------------------

    /// `to_shortest_str` with `grisu::format_shortest`.
    static Formatted toShortestStr(
            boolean negative, FullDecoded fullDecoded, Sign sign,
            int fracDigits, byte[] buf, Part[] parts) {
        String signStr = determineSign(sign, fullDecoded, negative);
        int n;
        switch (fullDecoded) {
            case FullDecoded.Nan ignored -> {
                parts[0] = new Part.Copy(B_NAN, 0, B_NAN.length);
                n = 1;
            }
            case FullDecoded.Infinite ignored -> {
                parts[0] = new Part.Copy(B_INF, 0, B_INF.length);
                n = 1;
            }
            case FullDecoded.Zero ignored -> {
                if (fracDigits > 0) {
                    // [0.][0000]
                    parts[0] = new Part.Copy(B_0DOT, 0, B_0DOT.length);
                    parts[1] = new Part.Zero(fracDigits);
                    n = 2;
                } else {
                    parts[0] = new Part.Copy(B_0, 0, B_0.length);
                    n = 1;
                }
            }
            case FullDecoded.Finite f -> {
                FormatResult r = Grisu.formatShortest(f.d(), buf);
                n = digitsToDecStr(buf, r.len(), r.exp(), fracDigits, parts);
            }
        }
        return new Formatted(signStr, parts, n);
    }

    /// `to_shortest_exp_str` with `grisu::format_shortest`.
    static Formatted toShortestExpStr(
            boolean negative, FullDecoded fullDecoded, Sign sign,
            int decLo, int decHi, boolean upper, byte[] buf, Part[] parts) {
        String signStr = determineSign(sign, fullDecoded, negative);
        int n;
        switch (fullDecoded) {
            case FullDecoded.Nan ignored -> {
                parts[0] = new Part.Copy(B_NAN, 0, B_NAN.length);
                n = 1;
            }
            case FullDecoded.Infinite ignored -> {
                parts[0] = new Part.Copy(B_INF, 0, B_INF.length);
                n = 1;
            }
            case FullDecoded.Zero ignored -> {
                parts[0] = decLo <= 0 && 0 < decHi
                        ? new Part.Copy(B_0, 0, B_0.length)
                        : new Part.Copy(upper ? B_0EU0 : B_0E0, 0, 3);
                n = 1;
            }
            case FullDecoded.Finite f -> {
                FormatResult r = Grisu.formatShortest(f.d(), buf);
                int visExp = r.exp() - 1;
                n = decLo <= visExp && visExp < decHi
                        ? digitsToDecStr(buf, r.len(), r.exp(), 0, parts)
                        : digitsToExpStr(buf, r.len(), r.exp(), 0, upper, parts);
            }
        }
        return new Formatted(signStr, parts, n);
    }
}
