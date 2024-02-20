package io.silv.movie.presentation.movie

import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.MotionScene
import androidx.constraintlayout.compose.OnSwipe
import androidx.constraintlayout.compose.SwipeDirection
import androidx.constraintlayout.compose.SwipeMode
import androidx.constraintlayout.compose.SwipeSide
import androidx.constraintlayout.compose.Visibility

val playerScene = MotionScene {

    val player = createRefFor("player")
    val mainContainer = createRefFor("main_container")
    val scrollView2 = createRefFor("scroll_view_2")
    val closeImageView = createRefFor("close_image_view")
    val playImageView = createRefFor("play_image_view")
    val titleTextView = createRefFor("title_text_view")

    transition(
        name = "yt_transition",
        from = constraintSet("start") {
             constrain(player) {
                 width = Dimension.value(0.dp)
                 height = Dimension.value(0.dp)
                 bottom.linkTo(mainContainer.bottom)
                 end.linkTo(mainContainer.end)
                 start.linkTo(mainContainer.start)
                 top.linkTo(mainContainer.top)
             }
        },
        to = constraintSet("end") {
            constrain(scrollView2) {
                width = Dimension.value(0.dp)
                height = Dimension.value(1.dp)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
                horizontalBias = 0.5f
                start.linkTo(parent.start)
                bottom.linkTo(mainContainer.bottom)
                verticalBias = 1f
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
                width = Dimension.value(0.dp)
                height = Dimension.wrapContent
                alpha = 1f
                visibility = Visibility.Visible
                bottom.linkTo(player.bottom)
                end.linkTo(player.start, margin = 8.dp)
                start.linkTo(player.end, margin = 8.dp)
                top.linkTo(playImageView.top)
            }
        }
    ) {

        keyAttributes(closeImageView) { frame(90) { alpha = 0f } }
        keyAttributes(playImageView) { frame(90) { alpha = 0f } }
        keyAttributes(titleTextView) { frame(95) { alpha = 0f } }

        OnSwipe(
            anchor = mainContainer,
            side = SwipeSide.Bottom,
            dragScale = 6f,
            direction = SwipeDirection.Down,
            mode = SwipeMode.velocity(maxAcceleration = 40f)
        )
    }
}

val scene = MotionScene {

    val bottomNav = createRefFor("bottom_nav")
    val fragment = createRefFor("fragment")
    val container = createRefFor("container")

    transition(
        from = constraintSet {
           constrain(bottomNav) {

               width = Dimension.matchParent
               height = Dimension.wrapContent

               bottom.linkTo(parent.bottom)
               end.linkTo(parent.end)
               start.linkTo(parent.start)
           }

           constrain(fragment) {
               width = Dimension.value(0.dp)
               height = Dimension.value(0.dp)

               bottom.linkTo(bottomNav.top)
               end.linkTo(parent.end)
               start.linkTo(parent.start)
               top.linkTo(parent.top)
           }

           constrain(container) {
               width = Dimension.matchParent
               height = Dimension.matchParent

               bottom.linkTo(parent.bottom)
               end.linkTo(parent.end)
               start.linkTo(parent.start)

               translationZ = 20.dp
           }
        },
        to = constraintSet {

        }
    ) {


        keyAttributes(bottomNav) {
            frame(0) { translationY = 0.dp }
            frame(1) { translationY = 80.dp }
            frame(100) { translationY = 100.dp }
        }

        keyAttributes(container) {
            frame(30) { scaleY = 1f }
            frame(35) { translationY = 0.dp }
            frame(100) { translationY = -(80).dp }
        }
    }
}
