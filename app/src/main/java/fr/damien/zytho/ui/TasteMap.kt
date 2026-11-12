package fr.damien.zytho.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import fr.damien.zytho.data.Beer
import fr.damien.zytho.data.StyleGuide
import fr.damien.zytho.data.Tasting

/** Un point de la carte du goût. x : -4 (sucré) à +4 (amer) ; y : 0 (léger) à 4 (corsé). */
data class TastePoint(val x: Float, val y: Float, val rating: Double)

fun tastePoints(beers: List<Beer>, tastings: List<Tasting>): List<TastePoint> {
    val byId = beers.associateBy { it.id }
    return tastings.mapNotNull { t ->
        val style = byId[t.beerId]?.let { StyleGuide.of(it.style) }
        val sw = t.sweetness ?: style?.sweet ?: return@mapNotNull null
        val bi = t.bitterness ?: style?.bitter ?: return@mapNotNull null
        val bo = t.body ?: style?.body ?: return@mapNotNull null
        val jx = (((t.id * 37) % 11) - 5) / 25f
        val jy = (((t.id * 53) % 11) - 5) / 25f
        TastePoint((bi - sw).toFloat() + jx, bo.toFloat() + jy, t.rating)
    }
}

/** Centre de gravité des bières aimées (note ≥ 7), ou null. */
fun palateCenter(points: List<TastePoint>): Pair<Float, Float>? {
    val liked = points.filter { it.rating >= 7 }
    if (liked.isEmpty()) return null
    return liked.map { it.x }.average().toFloat() to liked.map { it.y }.average().toFloat()
}

@Composable
fun TasteMap(points: List<TastePoint>, center: Pair<Float, Float>?) {
    val bg = MaterialTheme.colorScheme.surfaceVariant
    val grid = MaterialTheme.colorScheme.outlineVariant
    Box(Modifier.fillMaxWidth().height(260.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val pad = 28.dp.toPx()
            val w = size.width
            val h = size.height
            drawRoundRect(bg, cornerRadius = CornerRadius(16.dp.toPx()))
            fun px(x: Float) = pad + (x.coerceIn(-4.5f, 4.5f) + 4.5f) / 9f * (w - 2 * pad)
            fun py(y: Float) = h - pad - (y.coerceIn(-0.5f, 4.5f) + 0.5f) / 5f * (h - 2 * pad)
            drawLine(grid, Offset(px(0f), pad / 2), Offset(px(0f), h - pad / 2), strokeWidth = 2f)
            drawLine(grid, Offset(pad / 2, py(2f)), Offset(w - pad / 2, py(2f)), strokeWidth = 2f)
            points.forEach { p ->
                val c = when {
                    p.rating >= 8 -> Color(0xFFE0A030)
                    p.rating >= 6 -> Color(0xFFC0703A)
                    else -> Color(0xFF8A7B6B)
                }
                val r = (5 + p.rating.toFloat()).dp.toPx() / 1.6f
                drawCircle(c.copy(alpha = 0.85f), radius = r, center = Offset(px(p.x), py(p.y)))
            }
            if (center != null) {
                val o = Offset(px(center.first), py(center.second))
                drawCircle(Color(0xFFF3E6CF), radius = 18.dp.toPx(), center = o, style = Stroke(width = 3.dp.toPx()))
                drawCircle(Color(0xFFF3E6CF), radius = 3.dp.toPx(), center = o)
            }
        }
        val label = MaterialTheme.typography.labelSmall
        val col = MaterialTheme.colorScheme.onSurfaceVariant
        Text("Sucré", style = label, color = col, modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp))
        Text("Amer", style = label, color = col, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp))
        Text("Corsé", style = label, color = col, modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
        Text("Léger", style = label, color = col, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
    }
}
