/// `just` — shared Java infrastructure library (no project-specific types).
///
/// Sub-packages:
/// - {@code just.math}: bit-exact f32 math ({@code F32}) and primitive
///   `Option<f32>` ({@code Opt}/{@code OptF32})
/// - {@code just.result}: {@code Result<T,E>} family
/// - {@code just.function}: `java.util.function` float counterparts
/// - {@code just.fmt}: Rust `f32`/`f64` Display/Debug formatting
///   ({@code RustFmt}, Grisu3 + Dragon4 fallback)
///
/// Dependency rule: code here may only depend on the JDK and JSpecify —
/// never on downstream consumers.
@NullMarked
package net.morphismmc.just;

import org.jspecify.annotations.NullMarked;
