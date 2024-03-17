package io.silv.movie.core

import java.io.Serializable

typealias MB = Long

val Long.MB : MB get() = this

fun MB.toBytes(): Long = this * 1000L

public data class Quad<out A, out B, out C, out D>(
    public val first: A,
    public val second: B,
    public val third: C,
    public val fourth: D
) : Serializable {

    /**
     * Returns string representation of the [Quad] including its [first], [second], [third] and [fourth] values.
     */
    public override fun toString(): String = "($first, $second, $third, $fourth)"
}