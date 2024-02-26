package io.silv.movie.core

import androidx.compose.runtime.Stable

@Stable
enum class Status {
    ReturningSeries {
        override fun toString(): String {
            return "Returning Series"
        }
    },
    Planned {
        override fun toString(): String {
            return "Planned"
        }
            },
    Pilot {
        override fun toString(): String {
            return "Pilot"
        }
          },
    InProduction {
        override fun toString(): String {
            return "In Production"
        }
                 },
    Ended {
        override fun toString(): String {
            return "Ended"
        }
          },
    Canceled {
        override fun toString(): String {
            return "Canceled"
        }
    },
    PostProduction {
        override fun toString(): String {
            return "Post Production"
        }
                   },
    Rumored{
        override fun toString(): String {
            return "Rumored"
        }
    },
    Released{
        override fun toString(): String {
            return "Released"
        }
    },
    None {
        override fun toString(): String {
            return "None"
        }
    };

    companion object {
        fun fromString(str: String): Status {
            return when(str) {
                "ReturningSeries" -> ReturningSeries
                "Planned" -> Planned
                "Pilot" -> Pilot
                "InProduction" -> InProduction
                "Ended" -> Ended
                "Canceled" -> Canceled
                "PostProduction" -> PostProduction
                "Rumored" -> Rumored
                "Released" -> Released
                else -> None
            }
        }
    }
}