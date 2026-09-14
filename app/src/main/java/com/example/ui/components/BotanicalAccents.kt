package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R

/**
 * Centralized Botanical Contrast Hierarchy.
 * Strictly enforces subtle low-contrast guidelines across screens and overlays.
 */
object BotanicalContrast {
    /** Full opacity for interactive / functional vector details (e.g. Quick Add icons) */
    const val FOREGROUND = 1.0f

    /** Visible opacity for empty state dedicated illustrations */
    const val SECONDARY = 0.85f

    /** Distinct corner branch accents */
    const val CORNER_BRANCH = 0.40f

    /** Distinct vertical timeline edge stems */
    const val FAINT_STEM = 0.35f

    /** Clearly perceptible background foliage silhouettes */
    const val BACKGROUND_SILHOUETTE = 0.22f
}

/**
 * Categorized usage types for Botanical overlays with pre-configured opacities and z-indices.
 */
enum class BotanicalUsageType(
    val defaultAlpha: Float,
    val defaultZIndex: Float
) {
    /** Barely visible background leaf silhouettes (0.08 alpha, zIndex -2) */
    BACKGROUND_SILHOUETTE(defaultAlpha = BotanicalContrast.BACKGROUND_SILHOUETTE, defaultZIndex = -2f),

    /** Faint vertical timeline edge stems (0.12 alpha, zIndex -1) */
    TIMELINE_STEM(defaultAlpha = BotanicalContrast.FAINT_STEM, defaultZIndex = -1f),

    /** Restrained top-corner subtle branch (0.20 alpha, zIndex -1) */
    CORNER_BRANCH(defaultAlpha = BotanicalContrast.CORNER_BRANCH, defaultZIndex = -1f),

    /** Mid-opacity dedicated illustrations for empty states or stats overview (0.50 alpha, zIndex 0) */
    ILLUSTRATION(defaultAlpha = BotanicalContrast.SECONDARY, defaultZIndex = 0f),

    /** Small restrained accents for widgets or card surfaces (0.50 alpha, zIndex 0) */
    ACCENT(defaultAlpha = BotanicalContrast.SECONDARY, defaultZIndex = 0f),

    /** Full opacity micro vector details for interactive components (1.0 alpha, zIndex 0) */
    INTERACTIVE(defaultAlpha = BotanicalContrast.FOREGROUND, defaultZIndex = 0f)
}

/**
 * Centralized, type-safe registry of Botanical Vector Drawable resource IDs
 * categorized strictly by use-case.
 */
object BotanicalAssets {
    /** Combined vector asset containing all botanical elements (branch, stem, silhouette, composition) */
    @DrawableRes val composite = R.drawable.botanical_assets

    /** Background & canvas overlay vectors and images */
    object Background {
        @DrawableRes val topBranch = R.drawable.img_top_right_coner_cascading_branch
        @DrawableRes val topBranchLeft = R.drawable.img_top_left_corner_cascading_branch
        @DrawableRes val timelineStem = R.drawable.ic_botanical_stem_timeline
        @DrawableRes val leafSilhouette = R.drawable.img_bottom_leaf_leaf_silhouete
        @DrawableRes val emptySpaceSilhouette = R.drawable.img_middle_right_silhouette
    }

    /** Small surface & interaction accents */
    object Accent {
        @DrawableRes val widget = R.drawable.ic_botanical_widget_accent
        @DrawableRes val quickAdd = R.drawable.ic_botanical_quick_add_detail
    }

    /** Richer thematic illustrations */
    object Illustration {
        @DrawableRes val statsComposition = R.drawable.img_bottom_centerpiece
        @DrawableRes val emptyState = R.drawable.ic_botanical_empty_illustration
        @DrawableRes val emptyStateArtwork = R.drawable.img_botanical_empty_state
        @DrawableRes val foliage = R.drawable.img_botanical_foliage
        @DrawableRes val heroBanner = R.drawable.img_botanical_banner
        @DrawableRes val heroCard = R.drawable.img_botanical_hero_card
    }
}

/**
 * Single Source of Truth for Botanical Vector Resources (Delegates to BotanicalAssets).
 */
object BotanicalDrawables {
    @DrawableRes val Composite = BotanicalAssets.composite
    @DrawableRes val TopCornerBranch = BotanicalAssets.Background.topBranch
    @DrawableRes val TopCornerBranchLeft = BotanicalAssets.Background.topBranchLeft
    @DrawableRes val TimelineEdgeStem = BotanicalAssets.Background.timelineStem
    @DrawableRes val EmptySpaceSilhouette = BotanicalAssets.Background.emptySpaceSilhouette
    @DrawableRes val StatsComposition = BotanicalAssets.Illustration.statsComposition
    @DrawableRes val BackgroundSilhouette = BotanicalAssets.Background.leafSilhouette
    @DrawableRes val WidgetAccent = BotanicalAssets.Accent.widget
    @DrawableRes val EmptyStateIllustration = BotanicalAssets.Illustration.emptyState
    @DrawableRes val EmptyStateArtwork = BotanicalAssets.Illustration.emptyStateArtwork
    @DrawableRes val QuickAddDetail = BotanicalAssets.Accent.quickAdd
    @DrawableRes val FoliageIllustration = BotanicalAssets.Illustration.foliage
    @DrawableRes val HeroBanner = BotanicalAssets.Illustration.heroBanner
    @DrawableRes val HeroCard = BotanicalAssets.Illustration.heroCard
}

/**
 * Wrapper Composable that automatically applies botanical vector assets to any container,
 * strictly enforcing z-index, opacity, and subordinate layout layering based on the usage type.
 */
@Composable
fun BotanicalWrapper(
    usageType: BotanicalUsageType,
    @DrawableRes drawableRes: Int,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopEnd,
    tint: Color? = null,
    customAlpha: Float? = null,
    customZIndex: Float? = null,
    assetModifier: Modifier = Modifier,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    val alpha = customAlpha ?: usageType.defaultAlpha
    val zIndex = customZIndex ?: usageType.defaultZIndex

    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = null,
            colorFilter = tint?.let { ColorFilter.tint(it) },
            contentScale = ContentScale.Fit,
            modifier = assetModifier
                .align(alignment)
                .alpha(alpha)
                .zIndex(zIndex)
        )
        content?.invoke(this)
    }
}

/**
 * Top Corner Subtle Branch Accent.
 * Positioned in top-right or top-left corners without interfering with title or status bar text.
 */
@Composable
fun TopCornerBranch(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    alpha: Float = BotanicalContrast.CORNER_BRANCH,
    size: Dp = 110.dp,
    mirrored: Boolean = false
) {
    val drawableRes = if (mirrored) BotanicalDrawables.TopCornerBranchLeft else BotanicalDrawables.TopCornerBranch
    Image(
        painter = painterResource(id = drawableRes),
        contentDescription = null,
        colorFilter = tint?.let { ColorFilter.tint(it) },
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .alpha(alpha)
            .zIndex(-1f)
    )
}

/**
 * Top Left Corner Subtle Branch Accent (Horizontally reflected cascading branch).
 */
@Composable
fun TopLeftCornerBranch(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    alpha: Float = BotanicalContrast.CORNER_BRANCH,
    size: Dp = 110.dp
) {
    TopCornerBranch(
        modifier = modifier,
        tint = tint,
        alpha = alpha,
        size = size,
        mirrored = true
    )
}

/**
 * Timeline Edge Faint Botanical Stem.
 * Placed behind vertical timeline lines to add subtle organic rhythm without obstructing text.
 */
@Composable
fun TimelineEdgeStem(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    alpha: Float = BotanicalContrast.FAINT_STEM
) {
    Image(
        painter = painterResource(id = BotanicalDrawables.TimelineEdgeStem),
        contentDescription = null,
        colorFilter = tint?.let { ColorFilter.tint(it) },
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .alpha(alpha)
            .zIndex(-1f)
    )
}

/**
 * Soft Botanical Silhouette for Empty Spaces or Card Backdrops.
 */
@Composable
fun EmptySpaceSilhouette(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    alpha: Float = BotanicalContrast.BACKGROUND_SILHOUETTE,
    size: Dp = 160.dp
) {
    Image(
        painter = painterResource(id = BotanicalDrawables.EmptySpaceSilhouette),
        contentDescription = null,
        colorFilter = tint?.let { ColorFilter.tint(it) },
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .alpha(alpha)
            .zIndex(-1f)
    )
}

/**
 * Larger Expressive Botanical Composition for Stats Screen & Overview areas.
 */
@Composable
fun StatsComposition(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    alpha: Float = BotanicalContrast.SECONDARY,
    size: Dp? = null
) {
    val sizingModifier = if (size != null) modifier.size(size) else modifier
    Image(
        painter = painterResource(id = BotanicalDrawables.StatsComposition),
        contentDescription = null,
        colorFilter = tint?.let { ColorFilter.tint(it) },
        contentScale = ContentScale.Fit,
        modifier = sizingModifier
            .alpha(alpha)
    )
}

/**
 * Barely Visible Leaf Silhouette for Navigation / Full Backgrounds.
 */
@Composable
fun BackgroundSilhouette(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    alpha: Float = BotanicalContrast.BACKGROUND_SILHOUETTE,
    size: Dp = 220.dp
) {
    Image(
        painter = painterResource(id = BotanicalDrawables.BackgroundSilhouette),
        contentDescription = null,
        colorFilter = tint?.let { ColorFilter.tint(it) },
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .alpha(alpha)
            .zIndex(-2f)
    )
}

/**
 * Small Botanical Accent for Compact Surfaces / Widget headers.
 */
@Composable
fun WidgetAccent(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    alpha: Float = BotanicalContrast.SECONDARY,
    size: Dp = 24.dp
) {
    Image(
        painter = painterResource(id = BotanicalDrawables.WidgetAccent),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier
            .size(size)
            .alpha(alpha)
    )
}

/**
 * Dedicated Botanical Empty State Component with Title and Subtitle.
 */
@Composable
fun BotanicalEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(170.dp),
            contentAlignment = Alignment.Center
        ) {
            // Distinct organic silhouette backdrop
            Image(
                painter = painterResource(id = BotanicalDrawables.EmptySpaceSilhouette),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .size(170.dp)
                    .alpha(0.35f)
            )

            // High-resolution generated botanical illustration artwork
            Image(
                painter = painterResource(id = BotanicalDrawables.EmptyStateArtwork),
                contentDescription = "Botanical seedling art",
                modifier = Modifier
                    .size(126.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        if (action != null) {
            Spacer(modifier = Modifier.height(20.dp))
            action()
        }
    }
}

/**
 * Micro Organic Vector Accent for Task Interaction / Quick Add headers.
 */
@Composable
fun QuickAddDetail(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Image(
        painter = painterResource(id = BotanicalDrawables.QuickAddDetail),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier
            .size(18.dp)
            .alpha(BotanicalContrast.FOREGROUND)
    )
}

/**
 * Full Screen Wrapper Container incorporating subtle background botanical accents.
 */
@Composable
fun BotanicalBackgroundContainer(
    modifier: Modifier = Modifier,
    showTopBranch: Boolean = true,
    showTopLeftBranch: Boolean = true,
    showBottomLeaf: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (showTopBranch) {
            TopCornerBranch(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-10).dp)
            )
        }

        if (showTopLeftBranch) {
            TopCornerBranch(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-20).dp, y = (-10).dp),
                mirrored = true
            )
        }

        if (showBottomLeaf) {
            BackgroundSilhouette(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-40).dp, y = 30.dp)
            )
        }

        content()
    }
}
