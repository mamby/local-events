package net.mamby.events.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import net.mamby.events.R

@Composable
internal fun feedFavoriteIconPainter(favorited: Boolean): Painter {
    if (!favorited) return painterResource(R.drawable.ic_lucide_bookmark)

    val bookmark = ImageVector.vectorResource(R.drawable.ic_lucide_bookmark)
    val path = bookmark.root[0] as VectorPath
    return rememberVectorPainter(
        defaultWidth = bookmark.defaultWidth,
        defaultHeight = bookmark.defaultHeight,
        viewportWidth = bookmark.viewportWidth,
        viewportHeight = bookmark.viewportHeight,
        name = bookmark.name,
        autoMirror = bookmark.autoMirror,
    ) { _, _ ->
        Path(
            pathData = path.pathData,
            pathFillType = path.pathFillType,
            fill = SolidColor(Color.Black),
            stroke = path.stroke,
            strokeAlpha = path.strokeAlpha,
            strokeLineWidth = path.strokeLineWidth,
            strokeLineCap = path.strokeLineCap,
            strokeLineJoin = path.strokeLineJoin,
            strokeLineMiter = path.strokeLineMiter,
        )
    }
}
