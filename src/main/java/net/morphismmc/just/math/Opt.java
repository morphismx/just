package net.morphismmc.just.math;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// JAFFY: Java-side support type (no upstream counterpart) — GAP-006.
//
// Marks a primitive position (field, parameter, return value, local variable,
// type argument slot) that carries Rust `Option<T>` semantics for a primitive
// `T`. The `None` sentinel depends on the carrier type's value range:
//
//   `@Opt float`            Option<f32>    OptF32.NONE (NaN payload 0x7FC07AFF)
//   `@Opt int`              Option<usize>/ Option<u32>/Option<i32>
//                           OptI32.NONE (-1); usize::MAX is unreachable in
//                           practice (Java array bound, GAP-014's tradeoff)
//   `@Opt int` + @Unsigned  Option<usize>/ Option<u16>/Option<u32>
//                           OptI32.NONE (-1); strictly safe for u16/u32
//                           (never a legal value); usize same as above
//   `@Opt int` (i16→int)    Option<i16>    OptI16.NONE (Integer.MIN_VALUE —
//                           widened carrier keeps MIN_VALUE strictly safe)
//   `@Opt long`             Option<NodeId> NONE = 0L (a real NodeId's
//                           version field is always non-zero, see GAP-015)
//
// `Option<i64>`/`Option<u64>`/`Option<i32>`/`Option<bool>` have no sentinel
// (full value range or no spare pattern) — use `@Nullable` wrappers instead.
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.CLASS)
public @interface Opt {
}
