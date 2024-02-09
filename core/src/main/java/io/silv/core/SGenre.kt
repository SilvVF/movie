package io.silv.core

interface SGenre {
    var id: Long?
    var name: String

    fun copy() = create().also {
        it.id = id
        it.name = name
    }

    companion object {
        fun create(): SGenre {
            return SGenreImpl()
        }
    }
}

class SGenreImpl : SGenre {
    override var id: Long? = null
    override var name: String = ""
}