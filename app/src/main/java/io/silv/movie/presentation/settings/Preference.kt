package io.silv.movie.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.silv.movie.R
import io.silv.movie.prefrences.core.Preference as PreferenceData

sealed class Preference {
    abstract val title: String
    abstract val enabled: Boolean

    sealed class PreferenceItem<T> : Preference() {
        abstract val subtitle: String?
        abstract val icon: ImageVector?
        abstract val onValueChanged: suspend (newValue: T) -> Boolean

        /**
         * A basic [PreferenceItem] that only displays texts.
         */
        data class TextPreference(
            override val title: String,
            override val subtitle: String? = null,
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true },

            val onClick: (() -> Unit)? = null,
        ) : PreferenceItem<String>()

        /**
         * A [PreferenceItem] that provides a two-state toggleable option.
         */
        data class SwitchPreference(
            val pref: PreferenceData<Boolean>,
            override val title: String,
            override val subtitle: String? = null,
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: Boolean) -> Boolean = { true },
        ) : PreferenceItem<Boolean>()

        /**
         * A [PreferenceItem] that provides a slider to select an integer number.
         */
        data class SliderPreference(
            val pref: PreferenceData<Int>,
            val min: Int = 0,
            val max: Int,
            val subtitleProvider: @Composable (value: Int) -> String,
            override val title: String = "",
            override val subtitle: String? = null,
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: Int) -> Boolean = { true },
        ) : PreferenceItem<Int>()

        /**
         * A [PreferenceItem] that provides a slider to select an integer number.
         */
        data class FloatSliderPreference(
            val pref: PreferenceData<Float>,
            val min: Float,
            val max: Float,
            val steps: Int,
            val subtitleProvider: @Composable (value: Float) -> String,
            override val title: String = "",
            override val subtitle: String? = null,
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: Float) -> Boolean = { true },
        ) : PreferenceItem<Float>()

        /**
         * A [PreferenceItem] that displays a list of entries as a dialog.
         */
        @Suppress("UNCHECKED_CAST")
        data class ListPreference<T>(
            val pref: PreferenceData<T>,
            override val title: String,
            override val subtitle: String? = "%s",
            val subtitleProvider: @Composable (value: T, entries: Map<T, String>) -> String? =
                { v, e -> subtitle?.format(e[v]) },
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: T) -> Boolean = { true },

            val entries: Map<T, String>,
        ) : PreferenceItem<T>() {
            internal suspend fun internalSet(newValue: Any) = pref.set(newValue as T)
            internal suspend fun internalOnValueChanged(newValue: Any) = onValueChanged(
                newValue as T,
            )

            @Composable
            internal fun internalSubtitleProvider(value: Any?, entries: Map<out Any?, String>) =
                subtitleProvider(value as T, entries as Map<T, String>)
        }

        /**
         * [ListPreference] but with no connection to a [PreferenceData]
         */
        data class BasicListPreference(
            val value: String,
            override val title: String,
            override val subtitle: String? = "%s",
            val subtitleProvider: @Composable (value: String, entries: Map<String, String>) -> String? =
                { v, e -> subtitle?.format(e[v]) },
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true },

            val entries: Map<String, String>,
        ) : PreferenceItem<String>()

        /**
         * A [PreferenceItem] that displays a list of entries as a dialog.
         * Multiple entries can be selected at the same time.
         */
        data class MultiSelectListPreference(
            val pref: PreferenceData<Set<String>>,
            override val title: String,
            override val subtitle: String? = "%s",
            val subtitleProvider: @Composable (
                value: Set<String>,
                entries: Map<String, String>,
            ) -> String? = { v, e ->
                val combined = remember(v) {
                    v.map { e[it] }
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString()
                } ?: stringResource(R.string.none)
                subtitle?.format(combined)
            },
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: Set<String>) -> Boolean = { true },

            val entries: Map<String, String>,
        ) : PreferenceItem<Set<String>>()

        /**
         * A [PreferenceItem] that shows a EditText in the dialog.
         */
        data class EditTextPreference(
            val pref: PreferenceData<String>,
            override val title: String,
            override val subtitle: String? = "%s",
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true },
        ) : PreferenceItem<String>()

        /**
         * A [PreferenceItem] that shows a multi-line EditText in the dialog.
         */
        data class MultiLineEditTextPreference(
            val pref: PreferenceData<String>,
            override val title: String,
            override val subtitle: String? = "%s",
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true },
        ) : PreferenceItem<String>()


        data class InfoPreference(
            override val title: String,
        ) : PreferenceItem<String>() {
            override val enabled: Boolean = true
            override val subtitle: String? = null
            override val icon: ImageVector? = null
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true }
        }

        data class CustomPreference(
            override val title: String,
            val content: @Composable (PreferenceItem<String>) -> Unit,
        ) : PreferenceItem<String>() {
            override val enabled: Boolean = true
            override val subtitle: String? = null
            override val icon: ImageVector? = null
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true }
        }
    }

    data class PreferenceGroup(
        override val title: String,
        override val enabled: Boolean = true,

        val preferenceItems: List<PreferenceItem<out Any>>,
    ) : Preference()
}