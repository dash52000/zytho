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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.damien.zytho.ZythoViewModel
import fr.damien.zytho.data.Beers
import fr.damien.zytho.data.StyleGuide

/** Après un scan, une photo ou une saisie : que fait-on de cette bière ? */
@Composable
fun ChooseScreen(
    vm: ZythoViewModel,
    onBack: () -> Unit,
    onBeer: (Long) -> Unit,
    onMode: (String) -> Unit,
) {
    val d = vm.draft
    val tastings by vm.tastings.collectAsState()
    val last = d.beerId?.let { id -> tastings.filter { it.beerId == id }.maxByOrNull { it.date } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Que fait-on ?") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Thumb(d.photoPath, 80.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(d.name.ifBlank { "Bière à identifier" }, style = MaterialTheme.typography.titleLarge)
                    val sub = listOf(d.breweryName, d.style, if (d.abv.isBlank()) "" else "${d.abv} %")
                        .filter { it.isNotBlank() }.joinToString(" · ")
                    if (sub.isNotEmpty()) Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (d.source.isNotBlank()) Text(d.source, style = MaterialTheme.typography.bodySmall, color = Copper)
                }
            }

            if (d.beerId != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        if (last != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Déjà goûtée", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                RatingBadge(last.rating)
                            }
                            Text("Le ${fmtDate(last.date)}" + if (last.aromas.isNotBlank()) " · ${last.aromas}" else "")
                            if (last.comment.isNotBlank()) Text("« ${last.comment} »")
                        } else {
                            Text("Connue mais jamais goûtée", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(onClick = { onMode("tasting") }, modifier = Modifier.fillMaxWidth()) { Text("Nouvelle dégustation") }
            FilledTonalButton(onClick = { onMode("cellar") }, modifier = Modifier.fillMaxWidth()) { Text("Ajouter en cave") }
            FilledTonalButton(onClick = { onMode("wish") }, modifier = Modifier.fillMaxWidth()) { Text("Ajouter à la wishlist") }
            val id = d.beerId
            if (id != null) {
                OutlinedButton(onClick = { onBeer(id) }, modifier = Modifier.fillMaxWidth()) { Text("Voir la fiche") }
            }
            val style = StyleGuide.of(d.style)
            Spacer(Modifier.height(8.dp))
            AnselmeBubble(
                if (style != null) "${style.emoji} Une ${style.name} ! ${style.tagline} En cave, je lui donne environ ${Beers.gardeYears(style.name)} an(s)."
                else "Je ne connais pas encore son style : indique-le dans la fiche, je te dirai quoi y chercher."
            )
        }
    }
}
