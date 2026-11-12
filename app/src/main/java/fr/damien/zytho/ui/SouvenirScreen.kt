@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package fr.damien.zytho.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import fr.damien.zytho.ZythoViewModel
import fr.damien.zytho.data.Anselme
import fr.damien.zytho.data.Beers
import fr.damien.zytho.data.StyleGuide
import java.io.File

/** La fiche souvenir et la réponse de Frère Anselme après une dégustation. */
@Composable
fun SouvenirScreen(
    vm: ZythoViewModel,
    tastingId: Long,
    onClose: () -> Unit,
    onBeer: (Long) -> Unit,
    onStyle: (Int) -> Unit,
    onEdit: (Long) -> Unit,
) {
    val tastings by vm.tastings.collectAsState()
    val beers by vm.beers.collectAsState()
    val breweries by vm.breweries.collectAsState()
    val t = tastings.firstOrNull { it.id == tastingId }
    val beer = t?.let { tt -> beers.firstOrNull { it.id == tt.beerId } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fiche souvenir") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Fermer") }
                },
                actions = {
                    if (t != null) {
                        IconButton(onClick = { onEdit(t.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Modifier") }
                    }
                },
            )
        },
    ) { pad ->
        if (t == null || beer == null) {
            Text("Dégustation introuvable.", modifier = Modifier.padding(pad).padding(24.dp))
            return@Scaffold
        }
        val brewery = beer.breweryId?.let { id -> breweries.firstOrNull { it.id == id } }
        val style = StyleGuide.of(beer.style)
        val aromas = t.aromas.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    val path = beer.photoPath
                    if (path != null && File(path).exists()) {
                        AsyncImage(
                            model = File(path), contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) { Text(style?.emoji ?: "🍺", fontSize = 72.sp) }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    ) {
                        Text(
                            Beers.fmtNote(t.rating),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
                Column(Modifier.padding(16.dp)) {
                    Text(beer.name, style = MaterialTheme.typography.headlineSmall)
                    Text(subtitleOf(beer, brewery), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        listOf(fmtDate(t.date), t.place, t.service).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Copper,
                    )
                    val robe = t.color
                    if (!robe.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            RobeSwatch(robe, 18.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Robe ${robe.lowercase()}")
                        }
                    }
                    if (aromas.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 10.dp),
                        ) {
                            aromas.forEach { a ->
                                Text(
                                    "${StyleGuide.icon(a)} ${a.lowercase()}",
                                    modifier = Modifier
                                        .padding(bottom = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                    val sw = t.sweetness
                    val bi = t.bitterness
                    val bo = t.body
                    val fz = t.fizz
                    if (sw != null || bi != null || bo != null || fz != null) {
                        Spacer(Modifier.height(8.dp))
                        if (sw != null) ProfileBar("Douceur", sw)
                        if (bi != null) ProfileBar("Amertume", bi, Copper)
                        if (bo != null) ProfileBar("Corps", bo)
                        if (fz != null) ProfileBar("Pétillance", fz, Copper)
                    }
                    if (t.comment.isNotBlank()) {
                        Text("« ${t.comment} »", modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            AnselmeBubble(Anselme.feedback(t, style, beer.name))

            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (style != null) {
                    FilledTonalButton(onClick = { onStyle(StyleGuide.indexOf(style.name)) }) {
                        Text("${style.emoji} Fiche ${style.name}")
                    }
                }
                FilledTonalButton(onClick = { onBeer(beer.id) }) { Text("La bière") }
                FilledTonalButton(onClick = { onEdit(t.id) }) { Text("Modifier") }
            }
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Santé !") }
        }
    }
}
