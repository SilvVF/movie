package io.silv.movie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import io.silv.movie.data.user.User

val LocalUser = compositionLocalOf<User?> { error("no user provided") }

@Composable
fun User?.rememberProfileImageData(): UserProfileImageData? {

    val currentUser = LocalUser.current

    return remember(this?.profileImage, currentUser) {
        this?.let {
            UserProfileImageData(
                userId = it.userId,
                isUserMe = it.userId == currentUser?.userId,
                path = it.profileImage
            )
        }
    }
}