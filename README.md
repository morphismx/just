# just

Shared support library for the Rust→Java porting effort — currently
consumed by `jaffy` (a Java port of taffy). It
holds the infra that a faithful port needs but Java lacks: primitive
`Option` sentinels, `Result`, `float` functional interfaces, and
byte-identical Rust float formatting. Dependency rule: JDK + JSpecify
only, never on downstream consumers.

## Packages

| Package | Contents |
|---|---|
| `just.math` | Bit-exact `f32` helpers (`F32`, `Sys`-level ops), primitive `Option` specializations (`Opt`/`OptF32`, `OptI16`, `OptI32`, `Unsigned`) |
| `just.result` | `Result<T,E>` — Rust-style `Ok`/`Err` sealed type |
| `just.function` | `java.util.function` counterparts for `float` (`FloatUnaryOperator`, `FloatBinaryOperator`, `FloatSupplier`, `FloatPredicate`, `ObjFloatFunction`, `ToFloatFunction`) |
| `just.fmt` | Byte-identical Rust float formatting: `RustFmt` (`{}`/`{:?}` for `f32`/`f64`, Grisu3 + Dragon4 ported from `rustc core::num::imp`) and `Ryu` (the `ryu` crate's `format_finite` pretty format — the exact text `serde_json` emits) |

## Use

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}
dependencies {
    api("net.morphismmc:just:1.0-SNAPSHOT")
}
```

## Build

```sh
./gradlew build                 # compile + test
./gradlew publishToMavenLocal   # install into ~/.m2 for downstream use
```

Requires JDK 25 (toolchain-managed).

## License & attribution

`NOTICE` records the upstream provenance: `just.fmt` ports `rustc 1.96.0`
core (`core::num::imp::*`, `core::fmt::float`) and the `ryu` crate's
`Buffer::format_finite`. Everything else is original work.
