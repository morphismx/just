package net.morphismmc.just.fmt;

/// `ryu` crate `Buffer::format_finite` (pretty format) for `f32` —
/// the exact text `serde_json` emits for an `f32` value.
///
/// Digit generation reuses the same Grisu3 shortest-digits path as
/// [`RustFmt`]; only the notation-selection rules differ (ryu chooses
/// between plain decimal and `d.ddde±k` by the decimal-point position,
/// unlike Rust `Display`/`Debug`).
///
/// Precondition: `v` must be finite — `format_finite` does not handle
/// NaN/infinity (serde_json writes `null` for those at the serializer
/// level; callers must do the same).
public final class Ryu {
    private Ryu() {}

    /// `format32` — ryu pretty format of a finite `f32`.
    ///
    /// `kk` = decimal-point position (value = digits × 10^(kk-len)),
    /// `k` = `kk - len` (exponent of the digit integer).
    public static String format(float v) {
        boolean sign = v < 0.0f || (v == 0.0f && Float.floatToRawIntBits(v) < 0);
        String prefix = sign ? "-" : "";
        if (v == 0.0f) {
            return prefix + "0.0";
        }

        byte[] buf = new byte[Flt2dec.MAX_SIG_DIGITS];
        Flt2dec.DecodeResult dr = Flt2dec.decodeF32(v);
        // Finite is guaranteed by the precondition; Grisu takes the mantissa
        // from the finite branch.
        Flt2dec.FullDecoded.Finite f = (Flt2dec.FullDecoded.Finite) dr.decoded();
        Flt2dec.FormatResult r = Grisu.formatShortest(f.d(), buf);
        int len = r.len();
        int kk = r.exp();
        int k = kk - len;
        StringBuilder sb = new StringBuilder(16).append(prefix);

        if (0 <= k && kk <= 13) {
            // 1234e7 -> "12340000000.0"
            appendDigits(sb, buf, 0, len);
            for (int i = len; i < kk; i++) {
                sb.append('0');
            }
            sb.append(".0");
        } else if (0 < kk && kk <= 13) {
            // 1234e-2 -> "12.34"
            appendDigits(sb, buf, 0, kk);
            sb.append('.');
            appendDigits(sb, buf, kk, len);
        } else if (-6 < kk && kk <= 0) {
            // 1234e-6 -> "0.001234"
            sb.append("0.");
            for (int i = 0; i < -kk; i++) {
                sb.append('0');
            }
            appendDigits(sb, buf, 0, len);
        } else if (len == 1) {
            // 1e30 -> "1e+30" (ryu pretty always signs a non-negative exponent)
            appendDigits(sb, buf, 0, 1);
            appendExp(sb, kk - 1);
        } else {
            // 1234e30 -> "1.234e+33"
            appendDigits(sb, buf, 0, 1);
            sb.append('.');
            appendDigits(sb, buf, 1, len);
            appendExp(sb, kk - 1);
        }
        return sb.toString();
    }

    private static void appendExp(StringBuilder sb, int exp) {
        sb.append('e');
        if (exp >= 0) {
            sb.append('+');
        }
        sb.append(exp);
    }

    private static void appendDigits(StringBuilder sb, byte[] buf, int off, int end) {
        for (int i = off; i < end; i++) {
            sb.append((char) (buf[i] & 0xFF));
        }
    }
}
