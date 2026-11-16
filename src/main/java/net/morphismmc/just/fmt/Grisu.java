package net.morphismmc.just.fmt;

// JAFFY: port of rustc 1.96.0 core::num::imp::flt2dec::strategy::grisu
// (shortest mode only — `format_shortest`/`format_shortest_opt`/`round_and_weed`).
//.
//
// Rust adaptation of the Grisu3 algorithm described in "Printing Floating-Point
// Numbers Quickly and Accurately with Integers" (Florian Loitsch, 2010).
// Falls back to Dragon when the representation would be inexact.
//
import org.jspecify.annotations.Nullable;

// Rust u64/u32 -> long/int with unsigned semantics (GAP-014).
final class Grisu {
    private Grisu() {}

    // see the comments in `format_shortest_opt` for the rationale.
    static final int ALPHA = -60;
    static final int GAMMA = -32;

    /// `(f, e, k)` triples of CACHED_POW10, flattened.
    private static final long[] CACHED_POW10 = {
        0xe61acf033d1a45dfL, -1087, -308,
        0xab70fe17c79ac6caL, -1060, -300,
        0xff77b1fcbebcdc4fL, -1034, -292,
        0xbe5691ef416bd60cL, -1007, -284,
        0x8dd01fad907ffc3cL, -980, -276,
        0xd3515c2831559a83L, -954, -268,
        0x9d71ac8fada6c9b5L, -927, -260,
        0xea9c227723ee8bcbL, -901, -252,
        0xaecc49914078536dL, -874, -244,
        0x823c12795db6ce57L, -847, -236,
        0xc21094364dfb5637L, -821, -228,
        0x9096ea6f3848984fL, -794, -220,
        0xd77485cb25823ac7L, -768, -212,
        0xa086cfcd97bf97f4L, -741, -204,
        0xef340a98172aace5L, -715, -196,
        0xb23867fb2a35b28eL, -688, -188,
        0x84c8d4dfd2c63f3bL, -661, -180,
        0xc5dd44271ad3cdbaL, -635, -172,
        0x936b9fcebb25c996L, -608, -164,
        0xdbac6c247d62a584L, -582, -156,
        0xa3ab66580d5fdaf6L, -555, -148,
        0xf3e2f893dec3f126L, -529, -140,
        0xb5b5ada8aaff80b8L, -502, -132,
        0x87625f056c7c4a8bL, -475, -124,
        0xc9bcff6034c13053L, -449, -116,
        0x964e858c91ba2655L, -422, -108,
        0xdff9772470297ebdL, -396, -100,
        0xa6dfbd9fb8e5b88fL, -369, -92,
        0xf8a95fcf88747d94L, -343, -84,
        0xb94470938fa89bcfL, -316, -76,
        0x8a08f0f8bf0f156bL, -289, -68,
        0xcdb02555653131b6L, -263, -60,
        0x993fe2c6d07b7facL, -236, -52,
        0xe45c10c42a2b3b06L, -210, -44,
        0xaa242499697392d3L, -183, -36,
        0xfd87b5f28300ca0eL, -157, -28,
        0xbce5086492111aebL, -130, -20,
        0x8cbccc096f5088ccL, -103, -12,
        0xd1b71758e219652cL, -77, -4,
        0x9c40000000000000L, -50, 4,
        0xe8d4a51000000000L, -24, 12,
        0xad78ebc5ac620000L, 3, 20,
        0x813f3978f8940984L, 30, 28,
        0xc097ce7bc90715b3L, 56, 36,
        0x8f7e32ce7bea5c70L, 83, 44,
        0xd5d238a4abe98068L, 109, 52,
        0x9f4f2726179a2245L, 136, 60,
        0xed63a231d4c4fb27L, 162, 68,
        0xb0de65388cc8ada8L, 189, 76,
        0x83c7088e1aab65dbL, 216, 84,
        0xc45d1df942711d9aL, 242, 92,
        0x924d692ca61be758L, 269, 100,
        0xda01ee641a708deaL, 295, 108,
        0xa26da3999aef774aL, 322, 116,
        0xf209787bb47d6b85L, 348, 124,
        0xb454e4a179dd1877L, 375, 132,
        0x865b86925b9bc5c2L, 402, 140,
        0xc83553c5c8965d3dL, 428, 148,
        0x952ab45cfa97a0b3L, 455, 156,
        0xde469fbd99a05fe3L, 481, 164,
        0xa59bc234db398c25L, 508, 172,
        0xf6c69a72a3989f5cL, 534, 180,
        0xb7dcbf5354e9beceL, 561, 188,
        0x88fcf317f22241e2L, 588, 196,
        0xcc20ce9bd35c78a5L, 614, 204,
        0x98165af37b2153dfL, 641, 212,
        0xe2a0b5dc971f303aL, 667, 220,
        0xa8d9d1535ce3b396L, 694, 228,
        0xfb9b7cd9a4a7443cL, 720, 236,
        0xbb764c4ca7a44410L, 747, 244,
        0x8bab8eefb6409c1aL, 774, 252,
        0xd01fef10a657842cL, 800, 260,
        0x9b10a4e5e9913129L, 827, 268,
        0xe7109bfba19c0c9dL, 853, 276,
        0xac2820d9623bf429L, 880, 284,
        0x80444b5e7aa7cf85L, 907, 292,
        0xbf21e44003acdd2dL, 933, 300,
        0x8e679c2f5e44ff8fL, 960, 308,
        0xd433179d9c8cb841L, 986, 316,
        0x9e19db92b4e31ba9L, 1013, 324,
        0xeb96bf6ebadf77d9L, 1039, 332,
    };

    static final int CACHED_POW10_FIRST_E = -1087;
    static final int CACHED_POW10_LAST_E = 1039;
    private static final int CACHED_POW10_LEN = 81;

    /// `(k, Fp)` result of `cached_power`.
    private record Cached(int k, Fp fp) {}

    /// `cached_power(alpha, gamma)` — returns `(minusk, cached)` such that
    /// `ALPHA <= -minusk + e <= GAMMA`.
    private static Cached cachedPower(int alpha, int gamma) {
        int offset = CACHED_POW10_FIRST_E;
        int range = CACHED_POW10_LEN - 1;
        int domain = CACHED_POW10_LAST_E - CACHED_POW10_FIRST_E;
        int idx = (gamma - offset) * range / domain;
        long f = CACHED_POW10[idx * 3];
        int e = (int) CACHED_POW10[idx * 3 + 1];
        int k = (int) CACHED_POW10[idx * 3 + 2];
        return new Cached(k, new Fp(f, e));
    }

    /// `max_pow10_no_more_than(x)` — `(kappa, 10^kappa)` with `10^k <= x < 10^(k+1)`.
    /// `x` is u32 (int); result packed as `kappa | (tenKappa << 8)` in a long,
    /// tenKappa in the high bits.
    private static long maxPow10NoMoreThan(int x) {
        int kappa;
        int ten;
        if (Integer.compareUnsigned(x, 10_000) < 0) {
            if (Integer.compareUnsigned(x, 100) < 0) {
                if (Integer.compareUnsigned(x, 10) < 0) {
                    kappa = 0; ten = 1;
                } else {
                    kappa = 1; ten = 10;
                }
            } else {
                if (Integer.compareUnsigned(x, 1000) < 0) {
                    kappa = 2; ten = 100;
                } else {
                    kappa = 3; ten = 1000;
                }
            }
        } else {
            if (Integer.compareUnsigned(x, 100_0000) < 0) {
                if (Integer.compareUnsigned(x, 10_0000) < 0) {
                    kappa = 4; ten = 10_000;
                } else {
                    kappa = 5; ten = 10_0000;
                }
            } else if (Integer.compareUnsigned(x, 1_0000_0000) < 0) {
                if (Integer.compareUnsigned(x, 1000_0000) < 0) {
                    kappa = 6; ten = 100_0000;
                } else {
                    kappa = 7; ten = 1000_0000;
                }
            } else {
                if (Integer.compareUnsigned(x, 10_0000_0000) < 0) {
                    kappa = 8; ten = 1_0000_0000;
                } else {
                    kappa = 9; ten = 10_0000_0000;
                }
            }
        }
        return kappa | ((long) ten << 32);
    }

    /// The shortest mode implementation for Grisu.
    /// Returns null when it would return an inexact representation.
    private static Flt2dec.@Nullable FormatResult formatShortestOpt(Flt2dec.Decoded d, byte[] buf) {
        if (!(d.mant() != 0 && d.minus() != 0 && d.plus() != 0)) {
            throw new AssertionError(); // JAFFY(GAP-010): Rust assert!
        }
        if (Long.compareUnsigned(d.mant() + d.plus(), d.mant()) < 0
                || Long.compareUnsigned(d.mant(), d.minus()) < 0) {
            throw new AssertionError(); // checked_add / checked_sub
        }
        if (Long.compareUnsigned(d.mant() + d.plus(), 1L << 61) >= 0) {
            throw new AssertionError(); // need at least three bits of additional precision
        }

        // start with the normalized values with the shared exponent
        Fp plus = new Fp(d.mant() + d.plus(), d.exp()).normalize();
        Fp minus = new Fp(d.mant() - d.minus(), d.exp()).normalizeTo(plus.e);
        Fp v = new Fp(d.mant(), d.exp()).normalizeTo(plus.e);

        // find any `cached = 10^minusk` such that `ALPHA <= minusk + plus.e + 64 <= GAMMA`.
        Cached c = cachedPower(ALPHA - plus.e - 64, GAMMA - plus.e - 64);
        int minusk = c.k();

        // scale fps. this gives the maximal error of 1 ulp (proved from Theorem 5.1).
        plus = plus.mul(c.fp());
        minus = minus.mul(c.fp());
        v = v.mul(c.fp());

        long plus1 = plus.f + 1;
        long minus1 = minus.f - 1;
        int e = -plus.e; // shared exponent (usize)

        // divide `plus1` into integral and fractional parts.
        int plus1int = (int) (plus1 >>> e); // u32
        long plus1frac = plus1 & ((1L << e) - 1);

        // calculate the largest `10^max_kappa` no more than `plus1`.
        long mp = maxPow10NoMoreThan(plus1int);
        int maxKappa = (int) (mp & 0xFF);
        int tenKappa = (int) (mp >>> 32); // u32

        int i = 0;
        int exp = maxKappa - minusk + 1;

        long delta1 = plus1 - minus1;
        long delta1frac = delta1 & ((1L << e) - 1);

        // render integral parts, while checking for the accuracy at each step.
        int remainder = plus1int; // u32
        while (true) {
            // divide `remainder` by `10^kappa`.
            int q = Integer.divideUnsigned(remainder, tenKappa);
            int r = Integer.remainderUnsigned(remainder, tenKappa);
            buf[i] = (byte) ('0' + q);
            i += 1;

            long plus1rem = ((r & 0xFFFFFFFFL) << e) + plus1frac; // (plus1 % 10^kappa) * 2^e
            if (Long.compareUnsigned(plus1rem, delta1) < 0) {
                // we've found the correct `kappa`.
                long tenKappaScaled = (tenKappa & 0xFFFFFFFFL) << e;
                return roundAndWeed(buf, i, exp, plus1rem, delta1, plus1 - v.f, tenKappaScaled, 1);
            }

            // break the loop when we have rendered all integral digits.
            if (i > maxKappa) {
                break;
            }

            // restore invariants
            tenKappa = Integer.divideUnsigned(tenKappa, 10);
            remainder = r;
        }

        // render fractional parts, while checking for the accuracy at each step.
        long rem = plus1frac;
        long threshold = delta1frac;
        long ulp = 1;
        while (true) {
            rem *= 10;
            threshold *= 10;
            ulp *= 10;

            // divide `remainder` by `10^kappa` (implicit).
            long q = rem >>> e;
            long r = rem & ((1L << e) - 1);
            buf[i] = (byte) ('0' + q);
            i += 1;

            if (Long.compareUnsigned(r, threshold) < 0) {
                long tenKappaScaled = 1L << e;
                return roundAndWeed(buf, i, exp, r, threshold, (plus1 - v.f) * ulp, tenKappaScaled, ulp);
            }

            // restore invariants
            rem = r;
        }
    }

    /// `round_and_weed` — returns the result or null (None) when inexact.
    private static Flt2dec.@Nullable FormatResult roundAndWeed(
            byte[] buf, int len, int exp,
            long remainder, long threshold, long plus1v, long tenKappa, long ulp) {
        if (len == 0) {
            throw new AssertionError();
        }

        long plus1vDown = plus1v + ulp; // plus1 - (v - 1 ulp)
        long plus1vUp = plus1v - ulp;   // plus1 - (v + 1 ulp)

        // decrease the last digit and stop at the closest representation to `v + 1 ulp`.
        long plus1w = remainder;
        while (Long.compareUnsigned(plus1w, plus1vUp) < 0
                && Long.compareUnsigned(threshold - plus1w, tenKappa) >= 0
                && (Long.compareUnsigned(plus1w + tenKappa, plus1vUp) < 0
                    || Long.compareUnsigned(plus1vUp - plus1w, plus1w + tenKappa - plus1vUp) >= 0)) {
            buf[len - 1] -= 1;
            plus1w += tenKappa;
        }

        // check if this representation is also the closest representation to `v - 1 ulp`.
        if (Long.compareUnsigned(plus1w, plus1vDown) < 0
                && Long.compareUnsigned(threshold - plus1w, tenKappa) >= 0
                && (Long.compareUnsigned(plus1w + tenKappa, plus1vDown) < 0
                    || Long.compareUnsigned(plus1vDown - plus1w, plus1w + tenKappa - plus1vDown) >= 0)) {
            return null;
        }

        // reject any `w(n)` not between `plus0` and `minus0`.
        if (Long.compareUnsigned(2 * ulp, plus1w) <= 0
                && Long.compareUnsigned(plus1w, threshold - 4 * ulp) <= 0) {
            return new Flt2dec.FormatResult(len, exp);
        }
        return null;
    }

    /// `format_shortest` — Grisu with Dragon fallback.
    static Flt2dec.FormatResult formatShortest(Flt2dec.Decoded d, byte[] buf) {
        Flt2dec.FormatResult r = formatShortestOpt(d, buf);
        return r != null ? r : Dragon.formatShortest(d, buf);
    }
}
