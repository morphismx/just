package net.morphismmc.just.fmt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

// JAFFY: unit tests for RustFmt (GAP-016), values from the GAPS.md
// rustc-1.96-vs-JDK-25 measured table plus boundary cases.
class RustFmtTest {

    private static void check(int bits, String display, String debug) {
        float v = Float.intBitsToFloat(bits);
        assertEquals(display, RustFmt.display(v), "display " + Integer.toHexString(bits));
        assertEquals(debug, RustFmt.debug(v), "debug " + Integer.toHexString(bits));
    }

    private static void check64(long bits, String display, String debug) {
        double v = Double.longBitsToDouble(bits);
        assertEquals(display, RustFmt.display(v), "display " + Long.toHexString(bits));
        assertEquals(debug, RustFmt.debug(v), "debug " + Long.toHexString(bits));
    }

    @Test
    void gap016MeasuredTable() {
        check(0x3f800000, "1", "1.0");
        check(0x42c80000, "100", "100.0");
        check(0x58635fa9, "1000000000000000", "1000000000000000.0");
        check(0x5a0e1bca, "10000000000000000", "1e16");
        check(0x38cf9e38, "0.000099", "9.9e-5");
        check(0x7f7fffff, "340282350000000000000000000000000000000", "3.4028235e38");
        check(0x00000001, "0.000000000000000000000000000000000000000000001", "1e-45");
        check(0x80000000, "-0", "-0.0");
        check(0x7f800000, "inf", "inf");
        check(0x7fc07aff, "NaN", "NaN");
    }

    @Test
    void boundaryCases() {
        check(0x00000000, "0", "0.0");
        check(0xbf800000, "-1", "-1.0");
        check(0xff800000, "-inf", "-inf");
        check(0xffc00000, "NaN", "NaN");
        check(0x007fffff, "0.000000000000000000000000000000000000011754942", "1.1754942e-38");
        check(0x00800000, "0.000000000000000000000000000000000000011754944", "1.1754944e-38");
        check(0x3dcccccd, "0.1", "0.1");
        check(0x3eaaaaab, "0.33333334", "0.33333334");
        check(0x3727c5ac, "0.00001", "1e-5");
        check(0x39d1b717, "0.0004", "0.0004");
        check(0x80000001, "-0.000000000000000000000000000000000000000000001", "-1e-45");
    }

    @Test
    void f64() {
        check64(0x0000000000000000L, "0", "0.0");
        check64(0x8000000000000000L, "-0", "-0.0");
        check64(0x3ff0000000000000L, "1", "1.0");
        check64(0x7ff0000000000000L, "inf", "inf");
        check64(0xfff0000000000000L, "-inf", "-inf");
        check64(0x7ff8000000000000L, "NaN", "NaN");
        check64(0x0000000000000001L,
                "0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005",
                "5e-324");
        check64(0x0010000000000000L,
                "0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000022250738585072014",
                "2.2250738585072014e-308");
        check64(0x7fefffffffffffffL,
                "179769313486231570000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                "1.7976931348623157e308");
    }
}
