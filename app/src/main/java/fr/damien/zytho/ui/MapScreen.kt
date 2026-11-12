@file:OptIn(ExperimentalMaterial3Api::class)

package fr.damien.zytho.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import fr.damien.zytho.ZythoViewModel
import fr.damien.zytho.data.Beers
import fr.damien.zytho.data.Brewery
import fr.damien.zytho.data.Place
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

/** Carte des brasseries goûtées (OpenStreetMap). */
@Composable
fun MapScreen(vm: ZythoViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val breweries by vm.breweries.collectAsState()
    val beers by vm.beers.collectAsState()
    val tastings by vm.tastings.collectAsState()
    var locating by remember { mutableStateOf<Brewery?>(null) }

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(8.0)
            controller.setCenter(GeoPoint(49.68, 5.55))
        }
    }
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Statistiques par brasserie
    val beersByBrewery = beers.groupBy { it.breweryId }
    val tastingsByBeer = tastings.groupBy { it.beerId }
    fun statsOf(b: Brewery): String {
        val bs = beersByBrewery[b.id].orEmpty()
        val ts = bs.flatMap { tastingsByBeer[it.id].orEmpty() }
        val avg = if (ts.isEmpty()) "" else " · moyenne " + Beers.fmtNote(Math.round(ts.map { it.rating }.average() * 10) / 10.0)
        return "${bs.size} bière(s), ${ts.size} dégustation(s)$avg"
    }
    val located = breweries.filter { it.lat != null && it.lon != null }
    val toPlace = breweries.filter { it.lat == null || it.lon == null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carte des brasseries") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxWidth().height(340.dp),
                update = { map ->
                    map.overlays.removeAll { it is Marker }
                    located.forEach { b ->
                        val m = Marker(map)
                        m.position = GeoPoint(b.lat ?: 0.0, b.lon ?: 0.0)
                        m.title = (if (b.trappist) "✝ " else "") + b.name
                        m.snippet = statsOf(b)
                        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        map.overlays.add(m)
                    }
                    map.invalidate()
                },
            )
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Text(
                        if (located.isEmpty()) "Aucune brasserie placée pour l'instant. Localise-les ci-dessous."
                        else "${located.size} brasserie(s) sur la carte. Touche-en une pour y aller.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                items(located, key = { it.id }) { b ->
                    BreweryRow(b, statsOf(b), action = "Déplacer", onClick = {
                        mapView.controller.setZoom(13.0)
                        mapView.controller.animateTo(GeoPoint(b.lat ?: 0.0, b.lon ?: 0.0))
                    }, onAction = { locating = b })
                }
                if (toPlace.isNotEmpty()) {
                    item { SectionTitleRow("À placer (${toPlace.size})") }
                    items(toPlace, key = { it.id }) { b ->
                        BreweryRow(b, statsOf(b), action = "Localiser", onClick = { locating = b }, onAction = { locating = b })
                    }
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }

    locating?.let { b ->
        LocateDialog(
            vm = vm,
            brewery = b,
            onPickCenter = {
                val c = mapView.mapCenter
                vm.setBreweryLocation(b, c.latitude, c.longitude)
                locating = null
            },
            onPick = { p ->
                vm.setBreweryLocation(b, p.lat, p.lon)
                mapView.controller.setZoom(12.0)
                mapView.controller.animateTo(GeoPoint(p.lat, p.lon))
                locating = null
            },
            onDismiss = { locating = null },
        )
    }
}

@Composable
private fun SectionTitleRow(text: String) {
    Box(Modifier.padding(horizontal = 16.dp)) { SectionTitle(text) }
}

@Composable
private fun BreweryRow(b: Brewery, stats: String, action: String, onClick: () -> Unit, onAction: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                (if (b.trappist) "✝ " else "") + b.name + if (b.country.isNotBlank()) " · ${b.country}" else "",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(stats, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onAction) { Text(action) }
    }
    HorizontalDivider()
}

@Composable
private fun LocateDialog(
    vm: ZythoViewModel,
    brewery: Brewery,
    onPickCenter: () -> Unit,
    onPick: (Place) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(listOf("Brasserie", brewery.name, brewery.country).filter { it.isNotBlank() }.joinToString(" ")) }
    var results by remember { mutableStateOf<List<Place>?>(null) }
    var loading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Où est ${brewery.name} ?") },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    label = { Text("Nom, village…") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Button(onClick = {
                        loading = true
                        scope.launch {
                            results = vm.searchPlaces(query)
                            loading = false
                        }
                    }) { Text("Chercher") }
                    Spacer(Modifier.width(12.dp))
                    if (loading) CircularProgressIndicator(Modifier.height(24.dp).width(24.dp))
                }
                val r = results
                if (r != null) {
                    if (r.isEmpty()) {
                        Hint("Rien trouvé. Essaie juste le nom du village, ou place-la au centre de la carte.")
                    }
                    LazyColumn(Modifier.heightIn(max = 220.dp)) {
                        items(r) { p ->
                            Text(
                                p.label,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth().clickable { onPick(p) }.padding(vertical = 8.dp),
                            )
                            HorizontalDivider()
                        }
                    }
                }
                FilledTonalButton(onClick = onPickCenter, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Placer au centre de la carte")
                }
                Hint("Astuce : déplace d'abord la carte au bon endroit, puis utilise ce bouton.")
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}
