@file:OptIn(ExperimentalMaterial3Api::class)

package fr.damien.zytho.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Map
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.damien.zytho.ZythoViewModel
import fr.damien.zytho.data.Beers

@Composable
fun JournalScreen(vm: ZythoViewModel, onBeer: (Long) -> Unit, onMap: () -> Unit) {
    val beers by vm.beers.collectAsState()
    val breweries by vm.breweries.collectAsState()
    val tastings by vm.tastings.collectAsState()
    val aromas by vm.aromas.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    var style by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("") }
    var minNote by rememberSaveable { mutableStateOf("") }
    var aroma by rememberSaveable { mutableStateOf("") }

    val rows = remember(beers, breweries, tastings) {
        beerRows(beers, breweries, tastings).filter { it.last != null }.sortedByDescending { it.last?.date ?: 0L }
    }
    val countries = remember(breweries) { breweries.map { it.country }.filter { it.isNotBlank() }.distinct().sorted() }
    val usedAromas = remember(tastings, aromas) {
        val used = tastings.flatMap { t -> t.aromas.split(",").map { it.trim() } }.filter { it.isNotEmpty() }.toSet()
        aromas.map { it.label }.filter { it in used }
    }

    val q = query.trim().lowercase()
    val min = minNote.removePrefix("≥ ").toDoubleOrNull()
    val filtered = rows.filter { r ->
        (q.isEmpty() || r.beer.name.lowercase().contains(q) || (r.brewery?.name?.lowercase()?.contains(q) == true)) &&
            (style.isEmpty() || r.beer.style == style) &&
            (country.isEmpty() || r.brewery?.country == country) &&
            (min == null || (r.last?.rating ?: 0.0) >= min) &&
            (aroma.isEmpty() || r.last?.aromas?.split(",")?.map { it.trim() }?.contains(aroma) == true)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp),
        ) {
            Text("Carnet", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onMap) { Icon(Icons.Filled.Map, contentDescription = "Carte des brasseries", tint = Amber) }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Déjà goûtée ? Cherche une bière…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Effacer") }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            FilterDropdown("Style", style, Beers.STYLES) { style = it }
            FilterDropdown("Pays", country, countries) { country = it }
            FilterDropdown("Note", minNote, listOf("≥ 6", "≥ 7", "≥ 8", "≥ 9")) { minNote = it }
            FilterDropdown("Arôme", aroma, usedAromas) { aroma = it }
        }
        Spacer(Modifier.height(4.dp))

        if (rows.isEmpty()) {
            Text(
                "Aucune dégustation pour l'instant.\nAppuie sur + pour scanner ta première bière.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        } else if (filtered.isEmpty()) {
            val other = if (q.isNotEmpty()) beers.firstOrNull { it.name.lowercase().contains(q) } else null
            Text(
                if (other != null) "« ${other.name} » est enregistrée mais jamais goûtée."
                else "Jamais goûtée (ou rien ne correspond aux filtres).",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered, key = { it.beer.id }) { r ->
                    val last = r.last
                    val extra = buildString {
                        if (last != null) append(fmtDate(last.date))
                        if (r.count > 1) append(" · ${r.count} dégustations")
                        if (r.beer.rebuy == "Oui") append(" · à racheter")
                    }
                    BeerCard(r.beer, r.brewery, onClick = { onBeer(r.beer.id) }, extra = extra) {
                        if (last != null) RatingBadge(last.rating)
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
