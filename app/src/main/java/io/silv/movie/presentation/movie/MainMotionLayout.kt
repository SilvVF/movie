package io.silv.movie.presentation.movie

import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.MotionScene
import androidx.constraintlayout.compose.Visibility

val playerScene = MotionScene {

    val player = createRefFor("player")
    val mainContainer = createRefFor("main_container")
    val scrollView = createRefFor("scroll_view")
    val closeImageView = createRefFor("close_image_view")
    val playImageView = createRefFor("play_image_view")
    val titleTextView = createRefFor("title_text_view")

    defaultTransition(
        from = constraintSet("start") {
             constrain(player) {
                 width = Dimension.wrapContent
                 height = Dimension.wrapContent
                 bottom.linkTo(mainContainer.bottom)
                 end.linkTo(mainContainer.end)
                 start.linkTo(mainContainer.start)
                 top.linkTo(mainContainer.top)
             }
            constrain(scrollView) {
                width = Dimension.wrapContent
                height = Dimension.fillToConstraints
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
                start.linkTo(parent.start)
                top.linkTo(mainContainer.bottom)
            }
            constrain(mainContainer) {
                width = Dimension.matchParent
                height = Dimension.ratio("16:9")
                end.linkTo(parent.end)
                horizontalBias = 0.5f
                start.linkTo(parent.start)
                top.linkTo(parent.top)
                verticalBias = 1f
            }

            constrain(closeImageView) {
                width = Dimension.wrapContent
                height = Dimension.wrapContent
                alpha = 0f
                visibility = Visibility.Gone
                bottom.linkTo(mainContainer.bottom)
                end.linkTo(mainContainer.end, margin = 16.dp)
                top.linkTo(mainContainer.top)
            }
            constrain(playImageView) {
                width = Dimension.wrapContent
                height = Dimension.wrapContent
                alpha = 0f
                visibility = Visibility.Gone
                bottom.linkTo(closeImageView.bottom)
                end.linkTo(closeImageView.start)
                top.linkTo(closeImageView.top)
            }
            constrain(titleTextView) {
                width = Dimension.value(0.dp)
                height = Dimension.wrapContent
                alpha = 0f
                visibility = Visibility.Gone
                bottom.linkTo(player.bottom)
                end.linkTo(player.start, margin = 8.dp)
                start.linkTo(player.end, margin = 8.dp)
                top.linkTo(playImageView.top)
            }
        },
        to = constraintSet("end") {
            constrain(scrollView) {
                width = Dimension.wrapContent
                height = Dimension.value(1.dp)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
                start.linkTo(parent.start)
                top.linkTo(mainContainer.bottom)
            }
            constrain(mainContainer) {
                width = Dimension.matchParent
                height = Dimension.value(54.dp)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
                horizontalBias = 0.5f
                start.linkTo(parent.start)
                top.linkTo(parent.top)
                verticalBias = 1f
            }
            constrain(player) {
                width = Dimension.value(100.dp)
                height = Dimension.value(0.dp)
                bottom.linkTo(mainContainer.bottom)
                start.linkTo(mainContainer.start)
                top.linkTo(mainContainer.top)
            }
            constrain(closeImageView) {
                width = Dimension.wrapContent
                height = Dimension.wrapContent
                alpha = 1f
                visibility = Visibility.Visible
                bottom.linkTo(mainContainer.bottom)
                end.linkTo(mainContainer.end, margin = 16.dp)
                top.linkTo(mainContainer.top)
            }
            constrain(playImageView) {
                width = Dimension.wrapContent
                height = Dimension.wrapContent
                alpha = 1f
                visibility = Visibility.Visible
                bottom.linkTo(closeImageView.bottom)
                end.linkTo(closeImageView.start)
                top.linkTo(closeImageView.top)
            }
            constrain(titleTextView) {
                width = Dimension.fillToConstraints
                height = Dimension.wrapContent
                alpha = 1f
                visibility = Visibility.Visible
                bottom.linkTo(playImageView.bottom)
                end.linkTo(playImageView.start, margin = 8.dp)
                start.linkTo(player.end, margin = 8.dp)
                top.linkTo(playImageView.top)
            }
        }
    ) {


        keyAttributes(closeImageView) { frame(90) { alpha = 0f } }
        keyAttributes(playImageView) { frame(90) { alpha = 0f } }
        keyAttributes(titleTextView) { frame(95) { alpha = 0f } }
    }
}

