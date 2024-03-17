package io.silv.movie.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource
import io.silv.movie.R

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
@Composable
fun Status.getString() = stringResource(
    id = when(this) {
        Status.ReturningSeries -> R.string.status_returning
        Status.Planned -> R.string.status_planned
        Status.Pilot -> R.string.status_pilot
        Status.InProduction -> R.string.status_in_production
        Status.Ended -> R.string.status_ended
        Status.Canceled -> R.string.status_canceled
        Status.PostProduction -> R.string.status_post_production
        Status.Rumored -> R.string.status_rumored
        Status.Released -> R.string.status_released
        Status.None -> R.string.status_none
    }
)