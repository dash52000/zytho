@file:OptIn(ExperimentalMaterial3Api::class)

package fr.damien.zytho.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.damien.zytho.data.Beer
import fr.damien.zytho.data.Beers
import fr.damien.zytho.data.Brewery
import fr.damien.zytho.data.Tasting
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Une bière avec son contexte, pour les listes. */
data class BeerRow(
    val beer: Beer,
    val brewery: Brewery?,
    val last: Tasting?,
    val count: Int,
    val avg: Double?,
)

fun beerRows(beers: List<Beer>, breweries: List<Brewery>, tastings: List<Tasting>): List<BeerRow> {
    val brById = breweries.associateBy { it.id }
    val byBeer = tastings.groupBy { it.beerId }
    return beers.map { b ->
        val ts = byBeer[b.id].orEmpty().sortedByDescending { it.date }
        BeerRow(
            beer = b,
            brewery = b.breweryId?.let { brById[it] },
            last = ts.firstOrNull(),
            count = ts.size,
            avg = if (ts.isEmpty()) null else ts.map { it.rating }.average(),
        )
    }
}

private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun fmtDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt)

fun subtitleOf(beer: Beer, brewery: Brewery?): String =
    listOfNotNull(
        brewery?.name?.takeIf { it.isNotBlank() },
        beer.style.takeIf { it.isNotBlank() },
        beer.abv?.let { Beers.fmtAbv(it) + " %" },
    ).joinToString(" · ")

@Composable
fun Thumb(path: String?, size: Dp) {
    val mod = Modifier.size(size).clip(RoundedCornerShape(10.dp))
    if (path != null && File(path).exists()) {
        AsyncImage(model = File(path), contentDescription = null, contentScale = ContentScale.Crop, modifier = mod)
    } else {
        Box(mod.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.LocalDrink, contentDescription = null, tint = Amber)
        }
    }
}

@Composable
fun RatingBadge(rating: Double) {
    Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(10.dp)) {
        Text(
            Beers.fmtNote(rating),
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun BeerCard(
    beer: Beer,
    brewery: Brewery?,
    onClick: () -> Unit,
    extra: String? = null,
    trailing: @Composable () -> Unit = {},
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Thumb(beer.photoPath, 60.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    beer.name, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                val sub = subtitleOf(beer, brewery)
                if (sub.isNotEmpty()) {
                    Text(
                        sub, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!extra.isNullOrBlank()) {
                    Text(extra, style = MaterialTheme.typography.bodySmall, color = Copper)
                }
            }
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/** Puce de filtre ouvrant une liste déroulante. "" = pas de filtre. */
@Composable
fun FilterDropdown(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = value.isNotEmpty(),
            onClick = { open = true },
            label = { Text(if (value.isEmpty()) label else value) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Tous") }, onClick = { onChange(""); open = false })
            options.forEach { o ->
                DropdownMenuItem(text = { Text(o) }, onClick = { onChange(o); open = false })
            }
        }
    }
}

/** Bouton qui ouvre une liste de choix (pour le style par exemple). */
@Composable
fun ChoiceButton(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (value.isEmpty()) label else "$label : $value")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(o) }, onClick = { onChange(o); open = false })
            }
        }
    }
}

@Composable
fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        options.forEach { o ->
            FilterChip(selected = selected == o, onClick = { onSelect(o) }, label = { Text(o) })
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = Amber,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
}
