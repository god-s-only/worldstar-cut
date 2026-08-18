package com.worldstar.cut.core.domain.result

/**
 * A generic wrapper that represents the result of any operation in the domain layer.
 * All repository calls and use cases return this type to give the presentation layer
 * a consistent, type-safe way to handle outcomes.
 *
 * @param T The type of data returned on success.
 */
sealed class Result<out T> {

    /** The operation completed successfully and produced [data]. */
    data class Success<out T>(val data: T) : Result<T>()

    /** The operation failed with a [failure] describing what went wrong. */
    data class Error(val failure: Failure) : Result<Nothing>()

    /** The operation is still in progress. Useful for Loading states in UI. */
    data object Loading : Result<Nothing>()

    // ─── Convenience helpers ────────────────────────────────────────────────

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    /** Returns the data if this is [Success], otherwise null. */
    fun getOrNull(): T? = (this as? Success)?.data

    /** Returns the data if [Success], or the result of [default] otherwise. */
    fun getOrElse(default: () -> @UnsafeVariance T): T = getOrNull() ?: default()

    /** Transforms the [Success] data without altering [Error] or [Loading]. */
    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error   -> this
        is Loading -> this
    }

    /** Runs [block] only when [Success]. Returns this for chaining. */
    fun onSuccess(block: (T) -> Unit): Result<T> {
        if (this is Success) block(data)
        return this
    }

    /** Runs [block] only when [Error]. Returns this for chaining. */
    fun onError(block: (Failure) -> Unit): Result<T> {
        if (this is Error) block(failure)
        return this
    }

    /** Runs [block] only when [Loading]. Returns this for chaining. */
    fun onLoading(block: () -> Unit): Result<T> {
        if (this is Loading) block()
        return this
    }
}

/**
 * Typed failures that bubble up from the data layer through domain to the UI.
 * Each sub-type carries enough context for the ViewModel to map it to a
 * user-facing message without leaking implementation details.
 */
sealed class Failure {

    /** A local I/O or database error (e.g. file not found, DB query failed). */
    data class LocalError(val message: String, val cause: Throwable? = null) : Failure()

    /** A network/API error. */
    data class NetworkError(val message: String, val cause: Throwable? = null) : Failure()

    /** The user denied a required permission. */
    data class PermissionDenied(val permission: String) : Failure()

    /** The requested media item was not found on disk. */
    data object MediaNotFound : Failure()

    /** The video/audio codec is not supported on this device. */
    data class UnsupportedCodec(val codec: String) : Failure()

    /** The FFmpeg processing command failed. */
    data class ProcessingError(val message: String, val returnCode: Int) : Failure()

    /** A premium-only feature was invoked without an active subscription. */
    data object PremiumRequired : Failure()

    /** An unexpected error not covered by the types above. */
    data class Unknown(val message: String, val cause: Throwable? = null) : Failure()

    /** Human-readable description for logging / debug UI. */
    fun toReadableMessage(): String = when (this) {
        is LocalError       -> "Local error: $message"
        is NetworkError     -> "Network error: $message"
        is PermissionDenied -> "Permission denied: $permission"
        is MediaNotFound    -> "Media item not found"
        is UnsupportedCodec -> "Unsupported codec: $codec"
        is ProcessingError  -> "Processing failed (code $returnCode): $message"
        is PremiumRequired  -> "Premium subscription required"
        is Unknown          -> "Unknown error: $message"
    }
}
