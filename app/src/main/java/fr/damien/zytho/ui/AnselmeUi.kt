package fr.damien.zytho.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.damien.zytho.data.Anselme
import fr.damien.zytho.data.StyleGuide

/** Petit portrait de Frère Anselme, dessiné. */
@Composable
fun MonkAvatar(size: Dp = 44.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(Color(0xFF6B4423), radius = w / 2, center = Offset(w / 2, h / 2))
        drawCircle(Color(0xFF4E2F17), radius = w * 0.36f, center = Offset(w / 2, h * 0.52f))
        drawCircle(Color(0xFFF2CDA0), radius = w * 0.27f, center = Offset(w / 2, h * 0.56f))
        drawCircle(Color(0xFFE8A88A), radius = w * 0.05f, center = Offset(w * 0.37f, h * 0.62f))
        drawCircle(Color(0xFFE8A88A), radius = w * 0.05f, center = Offset(w * 0.63f, h * 0.62f))
        drawCircle(Color(0xFF3B2414), radius = w * 0.03f, center = Offset(w * 0.42f, h * 0.53f))
        drawCircle(Color(0xFF3B2414), radius = w * 0.03f, center = Offset(w * 0.58f, h * 0.53f))
        drawArc(
            color = Color(0xFF3B2414), startAngle = 20f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(w * 0.40f, h * 0.56f), size = Size(w * 0.20f, h * 0.14f),
            style = Stroke(width = w * 0.03f),
        )
        drawCircle(Color(0xFFE0A030), radius = w * 0.13f, center = Offset(w * 0.82f, h * 0.82f))
        drawCircle(Color(0xFFF3E6CF), radius = w * 0.07f, center = Offset(w * 0.82f, h * 0.72f))
    }
}

/** Bulle de parole de Frère Anselme. */
@Composable
fun AnselmeBubble(lines: List<String>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        MonkAvatar(44.dp)
        Spacer(Modifier.width(10.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
            modifier = Modifier.weight(1f),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(Anselme.NAME, style = MaterialTheme.typography.labelMedium, color = Amber, fontWeight = FontWeight.Bold)
                lines.forEach { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun AnselmeBubble(text: String, modifier: Modifier = Modifier) = AnselmeBubble(listOf(text), modifier)

/** Pastille de couleur de robe. */
@Composable
fun RobeSwatch(robe: String, size: Dp = 16.dp) {
    val argb = StyleGuide.ROBES.firstOrNull { it.first == robe }?.second ?: return
    Box(Modifier.size(size).clip(CircleShape).background(Color(argb)))
}

/** Barre de profil 0..4 avec libellé. */
@Composable
fun ProfileBar(label: String, value: Int, color: Color = Amber) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(90.dp))
        LinearProgressIndicator(
            progress = { (value.coerceIn(0, 4) + 0.3f) / 4.3f },
            color = color,
            modifier = Modifier.weight(1f).height(8.dp),
        )
    }
}

@Composable
fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
