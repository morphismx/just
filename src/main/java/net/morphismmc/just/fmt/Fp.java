package net.morphismmc.just.fmt;

// JAFFY: port of rustc 1.96.0 core::num::imp::diy_float (Fp).
//.
//
// A custom 64-bit floating point type representing `f * 2^e`.
// `f` is u64 -> Java long with unsigned semantics (GAP-014).
final class Fp {
    /// The integer mantissa (u64).
    long f;
    /// The exponent in base 2 (i16).
    int e;

    Fp(long f, int e) {
        this.f = f;
        this.e = e;
    }

    /// Returns a correctly rounded product of itself and `other`.
    Fp mul(Fp other) {
        long lo = f * other.f;
        long hi = Math.unsignedMultiplyHigh(f, other.f); // JAFFY(GAP-014): u64 widening_mul
        long nf = hi + (lo >>> 63); /* round */
        int ne = e + other.e + 64;
        return new Fp(nf, ne);
    }

    /// Normalizes itself so that the resulting mantissa is at least `2^63`.
    Fp normalize() {
        int lz = Long.numberOfLeadingZeros(f);
        long nf = f << lz;
        int ne = e - lz;
        return new Fp(nf, ne);
    }

    /// Normalizes itself to have the shared exponent.
    /// It can only decrease the exponent (and thus increase the mantissa).
    Fp normalizeTo(int e) {
        int edelta = this.e - e;
        if (edelta < 0) {
            throw new AssertionError();
        }
        if ((f << edelta) >>> edelta != f) {
            throw new AssertionError();
        }
        return new Fp(f << edelta, e);
    }
}
