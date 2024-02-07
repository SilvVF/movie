package io.silv.core

typealias MB = Long

val Long.MB : MB get() = this

fun MB.toBytes(): Long = this * 1000L