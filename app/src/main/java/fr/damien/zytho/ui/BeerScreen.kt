@file:OptIn(ExperimentalMaterial3Api::class)

package fr.damien.zytho.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.damien.zytho.ZythoViewModel
import fr.damien.zytho.data.Beers
import fr.damien.zytho.data.StyleGuide
import fr.damien.zytho.data.Tasting

@Composable
fun BeerScreen(
    vm: ZythoViewModel,
    beerId: Long,
    onBack: () -> Unit,
    onAction: (String) -> Unit,
    onStyle: (Int) -> Unit,
    onSouvenir: (Long) -> Unit,
    onEditTasting: (Long) -> Unit,
) {
    val beers by vm.beers.collectAsState()
    val breweries by vm.breweries.collectAsState()
    val tastings by vm.tastings.collectAsState()
    val cellar by vm.cellar.collectAsState()
    val barcodes by vm.barcodes.collectAsState()

    val beer = beers.firstOrNull { it.id == beerId }
    var confirmDelete by remember { mutableStateOf(false) }
    var tastingToDelete by remember { mutableStateOf<Tasting?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(beer?.name ?: "Bière") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                },
                actions = {
                    IconButton(onClick = { onAction("beer") }) { Icon(Icons.Filled.Edit, contentDescription = "Modifier") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Supprimer") }
                },
            )
        },
    ) { pad ->
        if (beer == null) {
            Text("Bière introuvable.", modifier = Modifier.padding(pad).padding(24.dp))
            return@Scaffold
        }
        val brewery = beer.breweryId?.let { id -> breweries.firstOrNull { it.id == id } }
        val history = tastings.filter { it.beerId == beerId }.sortedByDescending { it.date }
        val bottles = cellar.filter { it.beerId == beerId }.sumOf { it.quantity }
        val codes = barcodes.filter { it.beerId == beerId }

        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Thumb(beer.photoPath, 110.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(beer.name, style = MaterialTheme.typography.titleLarge)
                        if (brewery != null) {
                            Text(
                                listOf(brewery.name, brewery.country).filter { it.isNotBlank() }.joinToString(" · "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            listOfNotNull(
                                beer.style.takeIf { it.isNotBlank() },
                                beer.abv?.let { Beers.fmtAbv(it) + " %" },
                            ).joinToString(" · "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (history.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RatingBadge(history.first().rating)
                                if (history.size > 1) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "moyenne " + Beers.fmtNote(history.map { it.rating }.average()),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
                val styleInfo = StyleGuide.of(beer.style)
                if (styleInfo != null) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        onClick = { onStyle(StyleGuide.indexOf(styleInfo.name)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            MonkAvatar(36.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${styleInfo.emoji} ${styleInfo.name} : ${styleInfo.tagline}", style = MaterialTheme.typography.bodyMedium)
                                Text("Lire la fiche du style →", style = MaterialTheme.typography.bodySmall, color = Amber)
                            }
                        }
                    }
                }
                SectionTitle("À racheter ?")
                ChipRow(Beers.REBUY, beer.rebuy) { vm.setRebuy(beer, it) }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onAction("tasting") }) { Text("Déguster") }
                    FilledTonalButton(onClick = { onAction("cellar") }) { Text("Cave +") }
                    FilledTonalButton(onClick = { onAction("wish") }) { Text("Wishlist") }
                }
                if (bottles > 0) {
                    Text(
                        "$bottles bouteille(s) en cave",
                        color = Copper,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (codes.isNotEmpty()) {
                    Text(
                        "Codes-barres : " + codes.joinToString { c -> if (c.format.isBlank()) c.code else "${c.code} (${c.format})" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                SectionTitle("Historique (${history.size})")
                if (history.isEmpty()) Text("Pas encore goûtée.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(history, key = { it.id }) { t ->
                Card(onClick = { onSouvenir(t.id) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        RatingBadge(t.rating)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                listOf(
                                    fmtDate(t.date), t.place, t.service, t.format,
                                    t.vintage?.let { "millésime $it" }.orEmpty(),
                                ).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (t.aromas.isNotBlank()) {
                                Text(
                                    t.aromas.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                        .joinToString("  ") { StyleGuide.icon(it) + " " + it.lowercase() },
                                    color = Amber, style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (t.comment.isNotBlank()) Text(t.comment, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column {
                            IconButton(onClick = { onEditTasting(t.id) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = Amber)
                            }
                            IconButton(onClick = { tastingToDelete = t }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (confirmDelete && beer != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer ${beer.name} ?") },
            text = { Text("La fiche, ses dégustations, sa cave et sa wishlist seront effacées.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteBeer(beer); onBack() }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } },
        )
    }
    tastingToDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { tastingToDelete = null },
            title = { Text("Supprimer cette dégustation ?") },
            text = { Text("${fmtDate(t.date)} · ${Beers.fmtNote(t.rating)}/10") },
            confirmButton = {
                TextButton(onClick = { vm.deleteTasting(t); tastingToDelete = null }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { tastingToDelete = null }) { Text("Annuler") } },
        )
    }
}
