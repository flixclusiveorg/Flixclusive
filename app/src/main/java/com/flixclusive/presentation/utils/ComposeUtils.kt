package com.flixclusive.presentation.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.navigation.NavHostController
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.navigation.popUpTo
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.spec.Route

object ComposeUtils {
    fun TextStyle.applyDropShadow(
        shadowColor: Color = Color.Black,
        offset: Offset = Offset(x = 2F, y = 4F),
        blurRadius: Float = 0.4F
    ) = this.copy(
        shadow = Shadow(
            color = shadowColor,
            offset = offset,
            blurRadius = blurRadius
        ),
    )

    fun NavHostController.navigateSingleTopTo(
        direction: Direction,
        route: Route
    ) = this.navigate(direction) {
        // Pop up to the start destination of the graph to
        // avoid building up a large stack of destinations
        // on the back stack as users select items
        popUpTo(route) {
            saveState = true
        }

        // Avoid multiple copies of the same destination when
        // re-selecting the same item
        launchSingleTop = true
        // Restore uiState when re-selecting a previously selected item
        restoreState = true
    }

    @Composable
    fun BorderedText(
        modifier: Modifier = Modifier,
        text: String,
        borderColor: Color,
        style: TextStyle
    ) {
        Box(
            modifier = modifier
        ) {
            Text(
                text = text,
                style = style
            )

            Text(
                text = text,
                color = borderColor,
                style = style.copy(
                    drawStyle = Stroke(
                        miter = 10F,
                        width = 3F,
                        join = StrokeJoin.Round
                    ),
                    background = Color.Transparent
                )
            )
        }
    }
}