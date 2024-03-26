package io.silv.movie.core

import java.io.Serializable

interface SCredit : Serializable {

    var id: Long
    var adult: Boolean
    var gender: Long
    var knownForDepartment: String
    var name: String
    var originalName: String
    var popularity: Double
    var profilePath: String?
    var character: String
    var creditId: String
    var crew: Boolean
    var order: Long?

    fun copy() = create().also {
        it.id = id
        it.adult = adult
        it.gender = gender
        it.knownForDepartment = knownForDepartment
        it.name = name
        it.originalName = originalName
        it.popularity = popularity
        it.profilePath = profilePath
        it.character = character
        it.creditId = creditId
        it.crew = crew
        it.order = order
    }

    companion object {
        fun create(): SCredit {
            return SCreditImpl()
        }
    }
}

class SCreditImpl(
    override var id: Long = -1L,
    override var adult: Boolean = false,
    override var gender: Long = -1L,
    override var knownForDepartment: String = "",
    override var name: String = "",
    override var originalName: String = "",
    override var popularity: Double = -1.0,
    override var profilePath: String? = null,
    override var character: String = "",
    override var creditId: String = "",
    override var crew: Boolean = false,
    override var order: Long? = null
) : SCredit