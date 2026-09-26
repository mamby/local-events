package net.mamby.events.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import net.mamby.events.R
import net.mamby.events.core.AttendanceSummaryText

@Composable
fun SvgIcon(
    name: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null
) {
    AsyncImage(
        model = iconAsset(name),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = colorFilter
    )
}

@Composable
fun RemoteOrAssetImage(
    source: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    AsyncImage(
        model = mediaModel(source),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

@Composable
fun FeedIconButton(
    iconName: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    targetWidth: Dp = LocalEventsDimens.FeedActionButtonSize,
    targetHeight: Dp = LocalEventsDimens.FeedActionButtonSize,
    iconSize: Dp = LocalEventsDimens.FeedActionIconSize
) {
    if (onLongClick == null) {
        IconButton(
            onClick = onClick,
            modifier = modifier
                .size(width = targetWidth, height = targetHeight)
                .semantics { this.contentDescription = contentDescription },
            enabled = enabled
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                SvgIcon(
                    name = iconName,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    colorFilter = null
                )
            }
        }
        return
    }

    val hapticFeedback = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .size(width = targetWidth, height = targetHeight)
            .clip(CircleShape)
            .semantics { this.contentDescription = contentDescription }
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClickLabel = onLongClickLabel,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        SvgIcon(
            name = iconName,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            colorFilter = null
        )
    }
}

@Composable
fun FeedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = LocalEventsColors.FeedAccent,
    textColor: Color = LocalEventsColors.FeedOnAccent,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.38f),
            disabledContentColor = textColor.copy(alpha = 0.5f)
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Text(
            text = text,
            style = LocalEventsTypography.FeedBadge.copy(
                color = if (enabled) textColor else textColor.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun StateMessage(
    title: String,
    subtitle: String? = null,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null,
    loading: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(44.dp),
                color = LocalEventsColors.FeedAccent
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = title,
            style = LocalEventsTypography.FeedPageTitle,
            overflow = TextOverflow.Ellipsis
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(text = subtitle, style = LocalEventsTypography.FeedPageSubtitle)
        }

        if (buttonText != null && onButtonClick != null) {
            Spacer(modifier = Modifier.height(14.dp))
            FeedButton(text = buttonText, onClick = onButtonClick)
        }
    }
}

@Composable
fun AttendanceSummaryRow(
    summary: AttendanceSummaryText,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = summary.contentDescription
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = summary.primaryText,
            modifier = if (summary.additionalCount > 0) Modifier.weight(1f, fill = false) else Modifier,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (summary.additionalCount > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "+${summary.additionalCount}",
                style = style,
                maxLines = 1
            )
        }
    }
}

fun iconAsset(name: String): String =
    "file:///android_asset/images/$name"

fun mediaModel(source: String): String =
    if (source.startsWith("http://", ignoreCase = true) ||
        source.startsWith("https://", ignoreCase = true)
    ) {
        source
    } else {
        iconAsset(source)
    }

fun shareText(context: android.content.Context, title: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TITLE, title)
        .putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(intent, title))
}
