package net.morphismmc.just.result;

import java.util.function.Function;
import org.jspecify.annotations.Nullable;

// JAFFY: Java-side support type (no upstream file counterpart) — GAP-011.
// Rust `Result<T, E>`: a sealed interface with `Ok`/`Err` record variants.
// `unwrap`/`expect` panic paths throw `IllegalStateException` per GAP-010;
// std-lib panic messages are not reproduced (accepted divergence).
public sealed interface Result<T, E> {

    record Ok<T, E>(T value) implements Result<T, E> {}
    record Err<T, E>(E error) implements Result<T, E> {}

    Ok<Void, ?> OK_UNIT = new Ok<>(null);

    /// `Ok(value)`
    static <T, E> Result<T, E> ok(T value) {
        return new Ok<>(value);
    }

    /// `Err(error)`
    static <T, E> Result<T, E> err(E error) {
        return new Err<>(error);
    }

    /// `Ok(())` — shared singleton per GAP-011.
    @SuppressWarnings("unchecked")
    static <E> Result<Void, E> okUnit() {
        return (Result<Void, E>) OK_UNIT;
    }

    /// `Result::is_ok`
    default boolean isOk() {
        return this instanceof Ok<T, E>;
    }

    /// `Result::is_err`
    default boolean isErr() {
        return this instanceof Err<T, E>;
    }

    /// `Result::ok` — `Option<T>` on a reference type is a nullable reference.
    default @Nullable T ok() {
        return this instanceof Ok<T, E> ok ? ok.value() : null;
    }

    /// `Result::unwrap` — panics on `Err`.
    default T unwrap() {
        if (this instanceof Ok<T, E> ok) {
            return ok.value();
        }
        throw new IllegalStateException();
    }

    /// `Result::expect` — panics on `Err`, keeping the caller's message.
    default T expect(String msg) {
        if (this instanceof Ok<T, E> ok) {
            return ok.value();
        }
        throw new IllegalStateException(msg);
    }

    /// `Result::unwrap_err` — panics on `Ok`.
    default E unwrapErr() {
        if (this instanceof Err<T, E> err) {
            return err.error();
        }
        throw new IllegalStateException();
    }

    /// `Result::expect_err` — panics on `Ok`, keeping the caller's message.
    default E expectErr(String msg) {
        if (this instanceof Err<T, E> err) {
            return err.error();
        }
        throw new IllegalStateException(msg);
    }

    /// `Result::unwrap_or`
    default T unwrapOr(T def) {
        return this instanceof Ok<T, E> ok ? ok.value() : def;
    }

    /// `Result::unwrap_or_else`
    default T unwrapOrElse(Function<? super E, ? extends T> f) {
        return this instanceof Ok<T, E> ok ? ok.value() : f.apply(((Err<T, E>) this).error());
    }

    /// `Result::map`
    default <U> Result<U, E> map(Function<? super T, ? extends U> f) {
        if (this instanceof Ok<T, E> ok) {
            return new Ok<>(f.apply(ok.value()));
        }
        return new Err<>(((Err<T, E>) this).error());
    }

    /// `Result::map_err`
    default <F> Result<T, F> mapErr(Function<? super E, ? extends F> f) {
        if (this instanceof Err<T, E> err) {
            return new Err<>(f.apply(err.error()));
        }
        return new Ok<>(((Ok<T, E>) this).value());
    }

    /// `Result::and_then`
    default <U> Result<U, E> andThen(Function<? super T, ? extends Result<U, E>> f) {
        if (this instanceof Ok<T, E> ok) {
            return f.apply(ok.value());
        }
        return new Err<>(((Err<T, E>) this).error());
    }
}
