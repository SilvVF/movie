package io.silv.movie.presentation.components.profile

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.DefaultModelEqualityDelegate
import coil.compose.EqualityDelegate
import coil.request.ImageRequest
import io.silv.movie.coil.fetchers.model.UserProfileImageData
import io.silv.movie.data.supabase.model.User
import io.silv.movie.presentation.LocalUser


@Composable
fun UserProfileImage(
    user: User? = LocalUser.current,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: Painter? = null,
    error: Painter? = null,//painterResource(id = R.drawable.user_default_proflie_icon),
    fallback: Painter? = error,
    onLoading: ((AsyncImagePainter.State.Loading) -> Unit)? = null,
    onSuccess: ((AsyncImagePainter.State.Success) -> Unit)? = null,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
    clipToBounds: Boolean = true,
    modelEqualityDelegate: EqualityDelegate = DefaultModelEqualityDelegate,
) {

    val imageData= remember(user?.profileImage) {
        user?.userId?.let {
            UserProfileImageData(
                userId = it,
                isUserMe = true,
                path = user.profileImage
            )
        }
    }

    AsyncImage(
        modifier = modifier
            .clip(CircleShape)
            .aspectRatio(1f),
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageData)
            .crossfade(true)
            .build(),
        error = error,
        fallback = fallback,
        onLoading = onLoading,
        onSuccess = onSuccess,
        onError = onError,
        contentDescription = contentDescription,
        contentScale = contentScale,
        alignment = alignment,
        alpha = alpha,
        colorFilter = colorFilter,
        filterQuality = filterQuality,
        clipToBounds = clipToBounds,
        modelEqualityDelegate = modelEqualityDelegate,
        placeholder = placeholder
    )
}

@Composable
fun UserProfileImage(
    data: UserProfileImageData,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: Painter? = null,
    error: Painter? = null,//painterResource(id = R.drawable.user_default_proflie_icon),
    fallback: Painter? = error,
    onLoading: ((AsyncImagePainter.State.Loading) -> Unit)? = null,
    onSuccess: ((AsyncImagePainter.State.Success) -> Unit)? = null,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
    clipToBounds: Boolean = true,
    modelEqualityDelegate: EqualityDelegate = DefaultModelEqualityDelegate,
) {
    AsyncImage(
        modifier = modifier
            .clip(CircleShape)
            .aspectRatio(1f),
        model = ImageRequest.Builder(LocalContext.current)
            .data(data)
            .crossfade(true)
            .build(),
        error = error,
        fallback = fallback,
        onLoading = onLoading,
        onSuccess = onSuccess,
        onError = onError,
        contentDescription = contentDescription,
        contentScale = contentScale,
        alignment = alignment,
        alpha = alpha,
        colorFilter = colorFilter,
        filterQuality = filterQuality,
        clipToBounds = clipToBounds,
        modelEqualityDelegate = modelEqualityDelegate,
        placeholder = placeholder
    )
}