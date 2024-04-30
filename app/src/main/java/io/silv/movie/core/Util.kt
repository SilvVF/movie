package io.silv.movie.core

import java.io.Serializable

typealias MB = Long

val Long.MB : MB get() = this

fun MB.toBytes(): Long = this * 1000L

data class Quad<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
) : Serializable {

    /**
     * Returns string representation of the [Quad] including its [first], [second], [third] and [fourth] values.
     */
    override fun toString(): String = "($first, $second, $third, $fourth)"
}

data class Penta<out A, out B, out C, out D, out E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
) : Serializable {

    /**
     * Returns string representation of the [Quad] including its [first], [second], [third], [fourth] and [fifth] values.
     */
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth)"
}