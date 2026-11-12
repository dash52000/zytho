package fr.damien.zytho.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.damien.zytho.ZythoViewModel
import fr.damien.zytho.data.CellarItem
import java.time.LocalDate

@Composable
fun CellarScreen(vm: ZythoViewModel, onBeer: (Long) -> Unit, onOpen: (CellarItem) -> Unit, showTitle: Boolean = true) {
    val cellar by vm.cellar.collectAsState()
    val beers by vm.beers.collectAsState()
    val breweries by vm.breweries.collectAsState()
    val year = LocalDate.now().year

    val beersById = beers.associateBy { it.id }
    val brById = breweries.associateBy { it.id }
    val items = cellar
        .filter { beersById.containsKey(it.beerId) }
        .sortedWith(compareBy<CellarItem> { it.drinkBefore ?: Int.MAX_VALUE }.thenBy { beersById[it.beerId]?.name })
    val total = items.sumOf { it.quantity }
    val urgent = items.filter { (it.drinkBefore ?: Int.MAX_VALUE) <= year }.sumOf { it.quantity }

    Column(Modifier.fillMaxSize()) {
        if (showTitle) {
            Text(
                "Cave",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            )
        }
        Text(
            if (total == 0) "Ta cave est vide. Scanne une bouteille et choisis « Ajouter en cave »."
            else "$total bouteille(s)" + if (urgent > 0) " · $urgent à boire cette année" else "",
            color = if (urgent > 0) Warn else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(items, key = { it.id }) { item ->
                val beer = beersById.getValue(item.beerId)
                val details = listOfNotNull(
                    "×${item.quantity}",
                    item.vintage?.toString(),
                    item.format.takeIf { it.isNotBlank() },
                    item.drinkBefore?.let { "avant $it" },
                ).joinToString(" · ")
                BeerCard(beer, beer.breweryId?.let { brById[it] }, onClick = { onBeer(beer.id) }, extra = details) {
                    Column(horizontalAlignment = Alignment.End) {
                        FilledTonalButton(onClick = { onOpen(item) }) { Text("Ouvrir") }
                        TextButton(onClick = { vm.changeQuantity(item, 1) }) { Text("+1") }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
