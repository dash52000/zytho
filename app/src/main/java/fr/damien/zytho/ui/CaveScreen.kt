@file:OptIn(ExperimentalMaterial3Api::class)

package fr.damien.zytho.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.damien.zytho.ZythoViewModel
import fr.damien.zytho.data.CellarItem
import fr.damien.zytho.data.WishItem

/** Onglet Cave : la cave de garde et la wishlist. */
@Composable
fun CaveScreen(
    vm: ZythoViewModel,
    onBeer: (Long) -> Unit,
    onOpen: (CellarItem) -> Unit,
    onTasted: (WishItem) -> Unit,
) {
    var showWish by rememberSaveable { mutableStateOf(false) }
    val cellar by vm.cellar.collectAsState()
    val wishes by vm.wishes.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Text(
            if (showWish) "Wishlist" else "Cave",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            FilterChip(
                selected = !showWish, onClick = { showWish = false },
                label = { Text("🍾 Cave (${cellar.sumOf { it.quantity }})") },
            )
            FilterChip(
                selected = showWish, onClick = { showWish = true },
                label = { Text("⭐ À goûter (${wishes.size})") },
            )
        }
        Box(Modifier.weight(1f)) {
            if (showWish) {
                WishScreen(vm, onBeer = onBeer, onTasted = onTasted, showTitle = false)
            } else {
                CellarScreen(vm, onBeer = onBeer, onOpen = onOpen, showTitle = false)
            }
        }
    }
}
