package io.silv.movie.database

import app.cash.sqldelight.ColumnAdapter


object DBAdapters {

    val listIntAdapter = object : ColumnAdapter<List<Int>, String> {
        override fun decode(databaseValue: String) =
            if (databaseValue.isEmpty()) {
                listOf()
            } else {
                databaseValue.split(",").mapNotNull { it.toIntOrNull() }
            }
        override fun encode(value: List<Int>) = value.joinToString(separator = ",")
    }

    val listStringAdapter = object : ColumnAdapter<List<String>, String> {
        override fun decode(databaseValue: String) =
            if (databaseValue.isEmpty()) {
                listOf()
            } else {
                databaseValue.split(",")
            }
        override fun encode(value: List<String>) = value.joinToString(separator = ",")
    }

}