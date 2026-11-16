package net.morphismmc.just.fmt;

// JAFFY: port of rustc 1.96.0 core::num::imp::bignum (Big32x40 only).
//.
//
// Stack-allocated arbitrary-precision integer: 32-bit digits x 40 = 1,280 bits.
// Rust `u32` digits -> Java int with unsigned semantics (GAP-014): all
// arithmetic goes through `& 0xFFFFFFFFL` or unsigned compares.
// Only the operations used by dragon::format_shortest are ported.
final class Big implements Comparable<Big> {

    private int size;
    private final int[] base = new int[40];

    /// Makes a bignum from one digit.
    static Big fromSmall(int v) {
        Big r = new Big();
        r.base[0] = v;
        r.size = 1;
        return r;
    }

    /// Makes a bignum from `u64` value.
    static Big fromU64(long v) {
        Big r = new Big();
        int sz = 0;
        while (v != 0) {
            r.base[sz] = (int) v;
            v >>>= 32;
            sz += 1;
        }
        r.size = sz;
        return r;
    }

    Big copy() {
        Big r = new Big();
        System.arraycopy(base, 0, r.base, 0, 40);
        r.size = size;
        return r;
    }

    /// Returns the internal digits slice `[a, b, c, ...]`.
    private int digitsLen() {
        return size;
    }

    /// Adds `other` to itself.
    Big add(Big other) {
        int sz = size >= other.size ? size : other.size; // cmp::max (usize)
        long carry = 0;
        for (int i = 0; i < sz; i++) {
            long t = (base[i] & 0xFFFFFFFFL) + (other.base[i] & 0xFFFFFFFFL) + carry;
            base[i] = (int) t;
            carry = t >>> 32;
        }
        if (carry != 0) {
            base[sz] = 1;
            sz += 1;
        }
        size = sz;
        return this;
    }

    /// Subtracts `other` from itself.
    Big sub(Big other) {
        int sz = size >= other.size ? size : other.size; // cmp::max (usize)
        long noborrow = 1; // !b + carry == ~b + 1 first step
        for (int i = 0; i < sz; i++) {
            long t = (base[i] & 0xFFFFFFFFL) + (~other.base[i] & 0xFFFFFFFFL) + noborrow;
            base[i] = (int) t;
            noborrow = t >>> 32;
        }
        if (noborrow != 1) {
            throw new AssertionError();
        }
        size = sz;
        return this;
    }

    /// Multiplies itself by a digit-sized `other`.
    Big mulSmall(int other) {
        int sz = size;
        long carry = 0;
        long ob = other & 0xFFFFFFFFL;
        for (int i = 0; i < sz; i++) {
            long t = (base[i] & 0xFFFFFFFFL) * ob + carry;
            base[i] = (int) t;
            carry = t >>> 32;
        }
        if (carry > 0) {
            base[sz] = (int) carry;
            sz += 1;
        }
        size = sz;
        return this;
    }

    /// Multiplies itself by `2^bits`.
    Big mulPow2(int bits) {
        int digits = bits / 32;
        bits = bits % 32;

        // shift by `digits * 32` bits
        for (int i = size - 1; i >= 0; i--) {
            base[i + digits] = base[i];
        }
        for (int i = 0; i < digits; i++) {
            base[i] = 0;
        }

        // shift by `bits` bits
        int sz = size + digits;
        if (bits > 0) {
            int last = sz;
            int overflow = base[last - 1] >>> (32 - bits);
            if (overflow > 0) {
                base[last] = overflow;
                sz += 1;
            }
            for (int i = last - 1; i > digits; i--) {
                base[i] = (base[i] << bits) | (base[i - 1] >>> (32 - bits));
            }
            base[digits] <<= bits;
        }

        size = sz;
        return this;
    }

    /// Multiplies itself by a number described by `other[0] + other[1] * 2^W + ...`.
    Big mulDigits(int[] other) {
        int otherLen = other.length;
        // the internal routine. works best when aa.length <= bb.length.
        int[] ret = new int[40];
        int retsz;
        if (size < otherLen) {
            retsz = mulInner(ret, base, size, other, otherLen);
        } else {
            retsz = mulInner(ret, other, otherLen, base, size);
        }
        System.arraycopy(ret, 0, base, 0, 40);
        size = retsz;
        return this;
    }

    private static int mulInner(int[] ret, int[] aa, int aaLen, int[] bb, int bbLen) {
        int retsz = 0;
        for (int i = 0; i < aaLen; i++) {
            int a = aa[i];
            if (a == 0) {
                continue;
            }
            int sz = bbLen;
            long carry = 0;
            long au = a & 0xFFFFFFFFL;
            for (int j = 0; j < bbLen; j++) {
                long t = au * (bb[j] & 0xFFFFFFFFL) + (ret[i + j] & 0xFFFFFFFFL) + carry;
                ret[i + j] = (int) t;
                carry = t >>> 32;
            }
            if (carry > 0) {
                ret[i + sz] = (int) carry;
                sz += 1;
            }
            if (retsz < i + sz) {
                retsz = i + sz;
            }
        }
        return retsz;
    }

    /// Lexicographic unsigned comparison of the digit arrays, most significant first.
    /// Returns -1, 0 or 1 like Rust's `cmp`.
    @Override
    public int compareTo(Big other) {
        int sz = size >= other.size ? size : other.size; // cmp::max (usize)
        for (int i = sz - 1; i >= 0; i--) {
            int c = Integer.compareUnsigned(base[i], other.base[i]);
            if (c != 0) {
                return c < 0 ? -1 : 1;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Big other)) {
            return false;
        }
        return java.util.Arrays.equals(base, other.base);
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(base);
    }
}
