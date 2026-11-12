package fr.damien.zytho.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.damien.zytho.ZythoViewModel
import fr.damien.zytho.data.WishItem

@Composable
fun WishScreen(vm: ZythoViewModel, onBeer: (Long) -> Unit, onTasted: (WishItem) -> Unit, showTitle: Boolean = true) {
    val wishes by vm.wishes.collectAsState()
    val beers by vm.beers.collectAsState()
    val breweries by vm.breweries.collectAsState()
    val beersById = beers.associateBy { it.id }
    val brById = breweries.associateBy { it.id }
    val items = wishes.filter { beersById.containsKey(it.beerId) }

    Column(Modifier.fillMaxSize()) {
        if (showTitle) {
            Text(
                "Wishlist",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            )
        }
        Text(
            if (items.isEmpty()) "Rien à goûter pour l'instant. En magasin, scanne et choisis « Wishlist »."
            else "${items.size} bière(s) à goûter",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(items, key = { it.id }) { w ->
                val beer = beersById.getValue(w.beerId)
                BeerCard(
                    beer, beer.breweryId?.let { brById[it] },
                    onClick = { onBeer(beer.id) },
                    extra = w.note.ifBlank { "ajoutée le " + fmtDate(w.addedAt) },
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        FilledTonalButton(onClick = { onTasted(w) }) { Text("Goûtée !") }
                        IconButton(onClick = { vm.deleteWish(w) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Retirer")
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
