package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Earthy Sage Natural Shape Tokens
 * Organic, tactile forms inspired by natural materials:
 * - Smooth stones, clay vessels, pressed leaves
 * - Soft asymmetry with functional purpose
 */
object ExpressiveShapeTokens {
    // Base shape scale
    val shapeExtraSmall = RoundedCornerShape(4.dp)
    val shapeSmall = RoundedCornerShape(8.dp)
    val shapeMedium = RoundedCornerShape(16.dp)
    val shapeLarge = RoundedCornerShape(24.dp)
    val shapeExtraLarge = RoundedCornerShape(32.dp)
    val shapePill = RoundedCornerShape(999.dp)

    // Organic asymmetric shapes for tactile interactions
    // Inspired by natural forms: smoothed stones, folded paper, clay impressions
    val shapeExpressiveCard = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = 10.dp,
        bottomEnd = 10.dp
    )
    
    val shapeSelectedTask = RoundedCornerShape(
        topStart = 22.dp,
        topEnd = 14.dp,
        bottomStart = 14.dp,
        bottomEnd = 22.dp
    )
    
    val shapeActiveNode = RoundedCornerShape(50)
    
    val shapeBottomSheet = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    val shapeQuickAdd = RoundedCornerShape(20.dp)
    
    // Natural organic shapes for special elements
    val shapeLeaf = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 24.dp,
        bottomStart = 24.dp,
        bottomEnd = 16.dp
    )
    
    val shapeStone = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 16.dp,
        bottomStart = 18.dp,
        bottomEnd = 22.dp
    )
}

val ExpressiveMaterialShapes = Shapes(
    extraSmall = ExpressiveShapeTokens.shapeExtraSmall,
    small = ExpressiveShapeTokens.shapeSmall,
    medium = ExpressiveShapeTokens.shapeMedium,
    large = ExpressiveShapeTokens.shapeLarge,
    extraLarge = ExpressiveShapeTokens.shapeExtraLarge
)

/**
 * Natural Motion Tokens
 * Soft, organic movement inspired by:
 * - Leaves settling, water flowing, clay being shaped
 * - Grounded, responsive, never jarring
 */
object ExpressiveMotionTokens {
    const val durationFast = 120
    const val durationShort = 180
    const val durationMedium = 320
    const val durationLong = 450

    // Natural easing curves
    val easingStandard: Easing = FastOutSlowInEasing
    val easingEmphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val easingEmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val easingDecelerate: Easing = LinearOutSlowInEasing
    
    // Gentle organic easing for natural feel
    val easingNatural: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

    // Spring physics for tactile interactions
    fun <T> springSoft() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun <T> springMedium() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> springStrong() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    fun <T> springNatural() = spring<T>(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessLow
    )

    fun <T> tweenMedium() = tween<T>(
        durationMillis = durationMedium,
        easing = easingEmphasized
    )
    
    fun <T> tweenShort() = tween<T>(
        durationMillis = durationShort,
        easing = easingNatural
    )
}
