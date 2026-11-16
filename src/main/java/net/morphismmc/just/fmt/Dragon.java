package net.morphismmc.just.fmt;

// JAFFY: port of rustc 1.96.0 core::num::imp::flt2dec::strategy::dragon
// (shortest mode only — `format_shortest`, `mul_pow10`, `div_rem_upto_16`).
//.
//
// Almost direct (but slightly optimized) Rust translation of Figure 3 of
// "Printing Floating-Point Numbers Quickly and Accurately" (Burger & Dybvig).
//
// `Ordering` comparisons are mapped to int: `a.cmp(&b) < rounding` becomes
// `a.compareTo(b) < rounding` with rounding = 1 (Greater) or 0 (Equal).
final class Dragon {
    private Dragon() {}

    private static final int[] POW10 = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000};
    // precalculated arrays of digits for 5^(2^n).
    private static final int[] POW5TO16 = {0x86f26fc1, 0x23};
    private static final int[] POW5TO32 = {0x85acef81, 0x2d6d415b, 0x4ee};
    private static final int[] POW5TO64 = {0xbf6a1f01, 0x6e38ed64, 0xdaa797ed, 0xe93ff9f4, 0x184f03};
    private static final int[] POW5TO128 = {
        0x2e953e01, 0x3df9909, 0xf1538fd, 0x2374e42f, 0xd3cff5ec, 0xc404dc08, 0xbccdb0da, 0xa6337f19,
        0xe91f2603, 0x24e,
    };
    private static final int[] POW5TO256 = {
        0x982e7c01, 0xbed3875b, 0xd8d99f72, 0x12152f87, 0x6bde50c6, 0xcf4a6e70, 0xd595d80f, 0x26b2716e,
        0xadc666b0, 0x1d153624, 0x3c42d35a, 0x63ff540e, 0xcc5573c0, 0x65f9ef17, 0x55bc28f2, 0x80dcc7f7,
        0xf46eeddc, 0x5fdcefce, 0x553f7,
    };

    /// `mul_pow10` — multiplies `x` by `10^n`.
    static void mulPow10(Big x, int n) {
        if (n < 8) {
            x.mulSmall(POW10[n & 7]);
            return;
        }
        // Multiply by the powers of 5 and shift the 2s in at the end.
        if ((n & 7) != 0) {
            x.mulSmall(POW10[n & 7] >>> (n & 7));
        }
        if ((n & 8) != 0) {
            x.mulSmall(POW10[8] >>> 8);
        }
        if ((n & 16) != 0) {
            x.mulDigits(POW5TO16);
        }
        if ((n & 32) != 0) {
            x.mulDigits(POW5TO32);
        }
        if ((n & 64) != 0) {
            x.mulDigits(POW5TO64);
        }
        if ((n & 128) != 0) {
            x.mulDigits(POW5TO128);
        }
        if ((n & 256) != 0) {
            x.mulDigits(POW5TO256);
        }
        x.mulPow2(n);
    }

    /// `div_rem_upto_16` — only usable when `x < 16 * scale`.
    private static int divRemUpto16(Big x, Big scale, Big scale2, Big scale4, Big scale8) {
        int d = 0;
        if (x.compareTo(scale8) >= 0) {
            x.sub(scale8);
            d += 8;
        }
        if (x.compareTo(scale4) >= 0) {
            x.sub(scale4);
            d += 4;
        }
        if (x.compareTo(scale2) >= 0) {
            x.sub(scale2);
            d += 2;
        }
        if (x.compareTo(scale) >= 0) {
            x.sub(scale);
            d += 1;
        }
        return d;
    }

    /// The shortest mode implementation for Dragon.
    static Flt2dec.FormatResult formatShortest(Flt2dec.Decoded d, byte[] buf) {
        if (!(d.mant() != 0 && d.minus() != 0 && d.plus() != 0)) {
            throw new AssertionError(); // JAFFY(GAP-010): Rust assert!
        }
        if (Long.compareUnsigned(d.mant() + d.plus(), d.mant()) < 0
                || Long.compareUnsigned(d.mant(), d.minus()) < 0) {
            throw new AssertionError(); // checked_add / checked_sub
        }

        // `a.cmp(&b) < rounding` is `if d.inclusive {a <= b} else {a < b}`
        int rounding = d.inclusive() ? 1 : 0; // Greater : Equal

        // estimate `k_0` from original inputs satisfying `10^(k_0-1) < high <= 10^(k_0+1)`.
        int k = Flt2dec.estimateScalingFactor(d.mant() + d.plus(), d.exp());

        // convert `{mant, plus, minus} * 2^exp` into the fractional form.
        Big mant = Big.fromU64(d.mant());
        Big minus = Big.fromU64(d.minus());
        Big plus = Big.fromU64(d.plus());
        Big scale = Big.fromSmall(1);
        if (d.exp() < 0) {
            scale.mulPow2(-d.exp());
        } else {
            mant.mulPow2(d.exp());
            minus.mulPow2(d.exp());
            plus.mulPow2(d.exp());
        }

        // divide `mant` by `10^k`. now `scale / 10 < mant + plus <= scale * 10`.
        if (k >= 0) {
            mulPow10(scale, k);
        } else {
            mulPow10(mant, -k);
            mulPow10(minus, -k);
            mulPow10(plus, -k);
        }

        // fixup when `mant + plus > scale` (or `>=`).
        if (scale.compareTo(mant.copy().add(plus)) < rounding) {
            // equivalent to scaling `scale` by 10
            k += 1;
        } else {
            mant.mulSmall(10);
            minus.mulSmall(10);
            plus.mulSmall(10);
        }

        // cache `(2, 4, 8) * scale` for digit generation.
        Big scale2 = scale.copy();
        scale2.mulPow2(1);
        Big scale4 = scale.copy();
        scale4.mulPow2(2);
        Big scale8 = scale.copy();
        scale8.mulPow2(3);

        boolean down;
        boolean up;
        int i = 0;
        while (true) {
            // generate one digit: `d[n] = floor(mant / scale) < 10`.
            int digit = divRemUpto16(mant, scale, scale2, scale4, scale8);
            buf[i] = (byte) ('0' + digit);
            i += 1;

            // - stop and round `down` when `mant < minus` (or `<=`).
            // - stop and round `up` when `scale < mant + plus` (or `<=`).
            down = mant.compareTo(minus) < rounding;
            up = scale.compareTo(mant.copy().add(plus)) < rounding;
            if (down || up) {
                break;
            }

            // restore the invariants.
            mant.mulSmall(10);
            minus.mulSmall(10);
            plus.mulSmall(10);
        }

        // rounding up happens when
        // i) only the rounding-up condition was triggered, or
        // ii) both conditions were triggered and tie breaking prefers rounding up.
        if (up && (!down || mant.mulPow2(1).compareTo(scale) >= 0)) {
            int c = Flt2dec.roundUp(buf, i);
            if (c >= 0) {
                buf[i] = (byte) c;
                i += 1;
                k += 1;
            }
        }

        return new Flt2dec.FormatResult(i, k);
    }
}
