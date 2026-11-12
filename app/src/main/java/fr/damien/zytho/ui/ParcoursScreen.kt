@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package fr.damien.zytho.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.damien.zytho.ZythoViewModel
import fr.damien.zytho.data.Parcours
import fr.damien.zytho.data.StyleGuide

@Composable
fun ParcoursScreen(vm: ZythoViewModel, onStyle: (Int) -> Unit, onScan: () -> Unit) {
    val beers by vm.beers.collectAsState()
    val tastings by vm.tastings.collectAsState()
    val done = Parcours.done(beers, tastings)
    val next = Parcours.next(beers, tastings)
    val total = StyleGuide.ALL.count { it.name != "Autre" }
    val doneNames = done.map { it.first.name }.toSet()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text(
            "Parcours",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        AnselmeBubble(
            when {
                done.isEmpty() -> "Bienvenue, mon ami. Je suis Frère Anselme. Nous allons explorer les bières ensemble, un style après l'autre. Pas d'examen : juste le plaisir de comprendre ce qu'il y a dans ton verre."
                next == null -> "Tu as goûté tous les styles que je connais. Il ne te reste plus qu'à recommencer… en comparant les millésimes."
                else -> "Tu as déjà exploré ${done.size} style(s) sur $total. Chaque dégustation affine ma connaissance de ton palais."
            }
        )

        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { done.size.toFloat() / total },
            color = Amber,
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )

        if (next != null) {
            val s = next.style
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Prochaine étape", style = MaterialTheme.typography.labelLarge, color = Amber)
                    Text("${s.emoji} ${s.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(s.tagline, style = MaterialTheme.typography.bodyMedium)
                    Text(next.reason, style = MaterialTheme.typography.bodySmall, color = Copper, modifier = Modifier.padding(top = 4.dp))
                    Text(s.lesson, modifier = Modifier.padding(top = 10.dp))
                    Text("À goûter", style = MaterialTheme.typography.labelLarge, color = Amber, modifier = Modifier.padding(top = 12.dp))
                    Text("Classique : ${s.classic}")
                    Text("Près de chez toi : ${s.local ?: "pas de référence locale pour ce style, le classique fera très bien l'affaire"}")
                    Hint("L'étape est validée dès que tu dégustes une bière de ce style.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Button(onClick = { onStyle(StyleGuide.indexOf(s.name)) }) { Text("Lire la leçon") }
                        FilledTonalButton(onClick = onScan) { Text("Scanner") }
                    }
                }
            }
        }

        if (done.isNotEmpty()) {
            SectionTitle("Ton chemin")
            done.forEach { (s, date) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStyle(StyleGuide.indexOf(s.name)) }
                        .padding(vertical = 8.dp),
                ) {
                    Text("✅", modifier = Modifier.width(32.dp))
                    Text("${s.emoji} ${s.name}", modifier = Modifier.weight(1f))
                    Text(fmtDate(date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        SectionTitle("Toutes les fiches de style")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StyleGuide.ALL.forEachIndexed { i, s ->
                FilterChip(
                    selected = s.name in doneNames,
                    onClick = { onStyle(i) },
                    label = { Text("${s.emoji} ${s.name}") },
                )
            }
        }
        Spacer(Modifier.height(90.dp))
    }
}
