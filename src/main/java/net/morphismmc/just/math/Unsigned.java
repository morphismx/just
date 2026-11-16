package net.morphismmc.just.math;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// JAFFY: Java-side support type (no upstream counterpart) — GAP-014.
//
// Marks a `byte`/`short`/`int`/`long` position (field, parameter, return
// value, local variable, type argument slot) that carries a Rust unsigned
// integer (`u8`/`u16`/`u32`/`u64`/`usize`). Java has no unsigned primitives;
// arithmetic/comparison/formatting must use the JDK unsigned helpers
// (`Integer.compareUnsigned`, `Long.toUnsignedString`, ...). Combined with
// `@Opt` it disambiguates the sentinel rule, e.g. `@Opt @Unsigned int` =
// `Option<usize>` with `OptI32.NONE`.
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.CLASS)
public @interface Unsigned {
}
