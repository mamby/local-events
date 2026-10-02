package net.mamby.events.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import net.mamby.androidkit.compose.theme.AndroidKitBottomSheetColors
import net.mamby.androidkit.compose.theme.AndroidKitComponentColors
import net.mamby.androidkit.compose.theme.AndroidKitSectionCardColors
import net.mamby.androidkit.compose.theme.AndroidKitTheme
import net.mamby.androidkit.compose.theme.AndroidKitThemeDefinition
import net.mamby.androidkit.compose.theme.AndroidKitThemes

object LocalEventsColors {
    val FeedBackground = Color(0xFF090B11)
    val FeedSurface = Color(0xFF11161E)
    val AppSurfaceLight = Color(0xFFFFFFFF)
    val AppPageBackgroundLight = Color(0xFFEEF1F5)
    val AppElevatedSurfaceLight = Color(0xFFFFFFFF)
    val AppElevatedSurfaceDark = Color(0xFF1B202A)
    val AppContentTextLight = Color(0xFF475467)
    val AppContentTextDark = Color(0xFFC8D0DE)
    val SettingsContextTextLight = Color(0xFF344054)
    val AppMutedTextLight = Color(0xFF667085)
    val AppMutedTextDark = Color(0xFF93A0B7)
    val FeedAccent = Color(0xFFFF6B4A)
    val FeedOnAccent = Color(0xFF11161E)
    val FeedAccentTextLight = Color(0xFFB7351B)
    val FeedAccentSoft = Color(0xFFFFC1B4)
    private val SelectionBlueLight = Color(0xFF007AFF)
    private val SelectionBlueDark = Color(0xFF0A84FF)
    val FeedTextPrimary = Color(0xFFF5F7FB)
    val FeedTextSecondary = Color(0xFFC8D0DE)
    val FeedTextMuted = Color(0xFF93A0B7)
    val FeedSearchPillBackground = Color(0x40090B11)
    val FeedBadgeBackground = Color(0xCC111824)
    val BottomSheetSurfaceLight = Color(0xFFFFFFFF)
    val BottomSheetSurfaceDark = Color(0xFF090B11)
    val EventDetailsTextPrimaryLight = Color(0xFF171A21)
    val EventDetailsTextSecondaryLight = Color(0xFF586274)
    val EventDetailsTextMutedLight = Color(0xFF747E90)
    val BlackScrim = Color(0x66000000)

    val FeedOverlayBrush: Brush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to Color(0x00000000),
            0.45f to Color(0x66090B11),
            1.00f to Color(0xE0090B11)
        )
    )

    fun selectionBlue(darkTheme: Boolean): Color =
        if (darkTheme) SelectionBlueDark else SelectionBlueLight
}

object LocalEventsDimens {
    val FeedOverlayHorizontalPadding = 12.dp
    val FeedOverlayTopPadding = 24.dp
    val FeedOverlayBottomPadding = 10.dp
    val FeedActionColumnEndPadding = 0.dp
    val FeedActionButtonSize = 48.dp
    val FeedActionIconSize = 26.dp
    val FeedSearchPillRadius = 24.dp
    val FeedTopOverlayHorizontalPadding = 12.dp
    val FeedTopOverlayTopPadding = 8.dp
    val FeedTopOverlayControlSpacing = 6.dp
    val FeedSettingsIconSize = 28.dp
    val FeedTopOverlayEndPadding = FeedActionColumnEndPadding
    val FeedSheetBottomPadding = 20.dp
    const val FeedSheetMaxHeightFraction = 0.90f
    val FavoritesCardHeight = 132.dp
    val FavoritesImageWidth = 72.dp
    val FavoritesImageHeight = 96.dp
}

object LocalEventsTypography {
    val AppFont = FontFamily.SansSerif

    val FeedPageTitle = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        color = LocalEventsColors.FeedTextPrimary
    )

    val FeedPageSubtitle = TextStyle(
        fontFamily = AppFont,
        fontSize = 14.sp,
        color = LocalEventsColors.FeedTextMuted
    )

    val FeedTitle = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        color = LocalEventsColors.FeedTextPrimary
    )

    val FeedMeta = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 0.4.sp,
        color = LocalEventsColors.FeedAccentSoft
    )

    val FeedDescription = TextStyle(
        fontFamily = AppFont,
        fontSize = 15.sp,
        color = LocalEventsColors.FeedTextSecondary
    )

    val FeedDetail = TextStyle(
        fontFamily = AppFont,
        fontSize = 13.sp,
        color = LocalEventsColors.FeedTextSecondary
    )

    val FeedSchedule = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = LocalEventsColors.FeedTextPrimary
    )

    val FeedBadge = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        color = LocalEventsColors.FeedTextPrimary
    )

    val SecondaryTitle = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp
    )

    val EventDetailsTitle = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp
    )

    val EventDetailsBody = TextStyle(
        fontFamily = AppFont,
        fontSize = 16.sp
    )

    val EventDetailsMeta = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 0.4.sp
    )

    val EventDetailsHint = TextStyle(
        fontFamily = AppFont,
        fontSize = 13.sp,
        color = LocalEventsColors.FeedTextMuted
    )

    val FormControl = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 1.1.em,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        color = LocalEventsColors.FeedTextMuted
    )
}

@Composable
fun LocalEventsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    floatingSurfaceOpacityLevel: Float = 0f,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) localEventsDarkColorScheme() else localEventsLightColorScheme()
    val componentTheme = if (darkTheme) AndroidKitThemes.Dark else AndroidKitThemes.Light
    val typography = MaterialTheme.typography.copy(
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = LocalEventsTypography.AppFont),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = LocalEventsTypography.AppFont),
        bodySmall = MaterialTheme.typography.bodySmall.copy(fontFamily = LocalEventsTypography.AppFont),
        titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = LocalEventsTypography.AppFont),
        titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = LocalEventsTypography.AppFont),
        titleSmall = MaterialTheme.typography.titleSmall.copy(fontFamily = LocalEventsTypography.AppFont)
    )
    val baseDefinition = AndroidKitThemeDefinition(
        floatingSurfaceOpacityLevel = floatingSurfaceOpacityLevel,
        colorScheme = colors,
        isDark = darkTheme,
    )
    val definition = baseDefinition.copy(
        componentColors = AndroidKitComponentColors(
            page = componentTheme.componentColors.page.copy(containerColor = componentTheme.colorScheme.background),
            bottomSheet = AndroidKitBottomSheetColors(
                containerColor = if (darkTheme) LocalEventsColors.BottomSheetSurfaceDark else LocalEventsColors.BottomSheetSurfaceLight,
                contentColor = if (darkTheme) LocalEventsColors.FeedTextPrimary else LocalEventsColors.EventDetailsTextPrimaryLight,
                dragHandleColor = if (darkTheme) LocalEventsColors.FeedTextMuted else Color(0xFFC1C7D2),
                scrimColor = LocalEventsColors.BlackScrim
            ),
            sectionCard = AndroidKitSectionCardColors(
                containerColor = if (darkTheme) LocalEventsColors.AppElevatedSurfaceDark else LocalEventsColors.AppElevatedSurfaceLight
            )
        )
    )

    AndroidKitTheme(
        definition = definition,
        content = { MaterialTheme(colorScheme = colors, typography = typography, content = content) }
    )
}

private fun localEventsDarkColorScheme(): ColorScheme =
    darkColorScheme(
        primary = LocalEventsColors.FeedAccent,
        background = LocalEventsColors.FeedBackground,
        surface = LocalEventsColors.FeedSurface,
        onPrimary = LocalEventsColors.FeedTextPrimary,
        onBackground = LocalEventsColors.FeedTextPrimary,
        onSurface = LocalEventsColors.FeedTextPrimary,
        onSurfaceVariant = LocalEventsColors.AppContentTextDark,
        outlineVariant = Color(0xFF283041)
    )

private fun localEventsLightColorScheme(): ColorScheme =
    lightColorScheme(
        primary = LocalEventsColors.FeedAccent,
        background = LocalEventsColors.AppPageBackgroundLight,
        surface = LocalEventsColors.AppSurfaceLight,
        onPrimary = LocalEventsColors.FeedTextPrimary,
        onBackground = LocalEventsColors.EventDetailsTextPrimaryLight,
        onSurface = LocalEventsColors.EventDetailsTextPrimaryLight,
        onSurfaceVariant = LocalEventsColors.AppMutedTextLight,
        outlineVariant = Color(0xFFD9DEE7)
    )
