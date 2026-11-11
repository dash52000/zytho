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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.damien.zytho.ZythoViewModel
import fr.damien.zytho.data.Beers

@Composable
fun StatsScreen(vm: ZythoViewModel, onBeer: (Long) -> Unit, onSettings: () -> Unit) {
    val beers by vm.beers.collectAsState()
    val breweries by vm.breweries.collectAsState()
    val tastings by vm.tastings.collectAsState()
    val cellar by vm.cellar.collectAsState()

    val rows = beerRows(beers, breweries, tastings).filter { it.last != null }
    val avg = if (tastings.isEmpty()) null else tastings.map { it.rating }.average()
    val top = rows.sortedByDescending { it.last?.rating ?: 0.0 }.take(5)
    val byStyle = rows.filter { it.beer.style.isNotBlank() }.groupBy { it.beer.style }
        .map { (style, rs) -> Triple(style, rs.size, rs.mapNotNull { it.last?.rating }.average()) }
        .sortedByDescending { it.second }
    val aromaCounts = tastings.flatMap { t -> t.aromas.split(",").map { it.trim() } }
        .filter { it.isNotEmpty() }.groupingBy { it }.eachCount()
        .entries.sortedByDescending { it.value }.take(8)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Text("Stats", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Réglages et export") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            StatTile("Dégustations", tastings.size.toString(), Modifier.weight(1f))
            StatTile("Bières", rows.size.toString(), Modifier.weight(1f))
            StatTile("Note moy.", avg?.let { Beers.fmtNote(Math.round(it * 10) / 10.0) } ?: "–", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            StatTile("Brasseries", breweries.size.toString(), Modifier.weight(1f))
            StatTile("En cave", cellar.sumOf { it.quantity }.toString(), Modifier.weight(1f))
            StatTile("À racheter", beers.count { it.rebuy == "Oui" }.toString(), Modifier.weight(1f))
        }

        SectionTitle("Top 5")
        if (top.isEmpty()) Text("Note quelques bières pour voir ton podium.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        top.forEachIndexed { i, r ->
            val last = r.last
            BeerCard(r.beer, r.brewery, onClick = { onBeer(r.beer.id) }, extra = "#${i + 1}") {
                if (last != null) RatingBadge(last.rating)
            }
        }

        if (byStyle.isNotEmpty()) {
            SectionTitle("Par style")
            val maxCount = byStyle.maxOf { it.second }
            byStyle.forEach { (style, n, mean) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(style, modifier = Modifier.width(130.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    LinearProgressIndicator(
                        progress = { n.toFloat() / maxCount },
                        color = Amber,
                        modifier = Modifier.weight(1f).height(8.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("$n · ${Beers.fmtNote(Math.round(mean * 10) / 10.0)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (aromaCounts.isNotEmpty()) {
            SectionTitle("Arômes les plus fréquents")
            val maxA = aromaCounts.first().value
            aromaCounts.forEach { (a, n) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(a, modifier = Modifier.width(130.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    LinearProgressIndicator(
                        progress = { n.toFloat() / maxA },
                        color = Copper,
                        modifier = Modifier.weight(1f).height(8.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("$n", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Text(
            "Radar d'arômes, timeline et carte des brasseries arrivent en V2.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 20.dp),
        )
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = Amber)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
