@file:OptIn(ExperimentalMaterial3Api::class)

package fr.damien.zytho.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.damien.zytho.ZythoViewModel
import kotlinx.coroutines.launch

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun ZythoApp(vm: ZythoViewModel = viewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val scope = rememberCoroutineScope()

    val tabs = listOf(
        Tab("journal", "Carnet", Icons.AutoMirrored.Filled.MenuBook),
        Tab("cave", "Cave", Icons.Filled.Inventory2),
        Tab("parcours", "Parcours", Icons.Filled.Explore),
        Tab("palais", "Palais", Icons.Filled.BarChart),
    )
    val onTab = tabs.any { it.route == route }
    var showAdd by remember { mutableStateOf(false) }
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }

    val takeLabelPhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingPhoto
        if (ok && path != null) {
            scope.launch {
                vm.draftFromLabel(path)
                nav.navigate("choose")
            }
        }
    }

    fun openBeer(id: Long) = nav.navigate("beer/$id")
    fun openStyle(index: Int) {
        if (index >= 0) nav.navigate("style/$index")
    }

    Scaffold(
        bottomBar = {
            if (onTab) {
                NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = {
                                nav.navigate(t.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (onTab) {
                FloatingActionButton(
                    onClick = { showAdd = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) { Icon(Icons.Filled.Add, contentDescription = "Ajouter") }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            NavHost(nav, startDestination = "journal") {
                composable("journal") { JournalScreen(vm, onBeer = ::openBeer) }
                composable("cave") {
                    CaveScreen(
                        vm,
                        onBeer = ::openBeer,
                        onOpen = { item ->
                            scope.launch {
                                vm.openBottle(item)
                                nav.navigate("edit/tasting")
                            }
                        },
                        onTasted = { w ->
                            scope.launch {
                                vm.tastedWish(w)
                                nav.navigate("edit/tasting")
                            }
                        },
                    )
                }
                composable("parcours") {
                    ParcoursScreen(vm, onStyle = ::openStyle, onScan = { nav.navigate("scan") })
                }
                composable("palais") { StatsScreen(vm, onBeer = ::openBeer, onSettings = { nav.navigate("settings") }) }
                composable(
                    "beer/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0L
                    BeerScreen(
                        vm, id,
                        onBack = { nav.popBackStack() },
                        onAction = { mode ->
                            scope.launch {
                                vm.prepareForBeer(id)
                                nav.navigate("edit/$mode")
                            }
                        },
                        onStyle = ::openStyle,
                        onSouvenir = { tid -> nav.navigate("souvenir/$tid") },
                    )
                }
                composable("scan") {
                    ScannerScreen(
                        busy = vm.busy,
                        onBack = { nav.popBackStack() },
                        onCode = { code ->
                            scope.launch {
                                vm.resolveBarcode(code)
                                nav.navigate("choose") { popUpTo("scan") { inclusive = true } }
                            }
                        },
                    )
                }
                composable("choose") {
                    ChooseScreen(
                        vm,
                        onBack = { nav.popBackStack() },
                        onBeer = { id -> nav.navigate("beer/$id") { popUpTo("choose") { inclusive = true } } },
                        onMode = { mode -> nav.navigate("edit/$mode") { popUpTo("choose") { inclusive = true } } },
                    )
                }
                composable("edit/{mode}") { entry ->
                    val mode = entry.arguments?.getString("mode") ?: "tasting"
                    EditScreen(
                        vm, mode,
                        onClose = { nav.popBackStack() },
                        onSavedTasting = { tid ->
                            nav.navigate("souvenir/$tid") { popUpTo("edit/{mode}") { inclusive = true } }
                        },
                    )
                }
                composable(
                    "souvenir/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0L
                    SouvenirScreen(
                        vm, id,
                        onClose = { nav.popBackStack() },
                        onBeer = { bid -> nav.navigate("beer/$bid") { popUpTo("souvenir/{id}") { inclusive = true } } },
                        onStyle = ::openStyle,
                    )
                }
                composable(
                    "style/{index}",
                    arguments = listOf(navArgument("index") { type = NavType.IntType }),
                ) { entry ->
                    StyleScreen(entry.arguments?.getInt("index") ?: 0, onBack = { nav.popBackStack() })
                }
                composable("settings") { SettingsScreen(vm, onBack = { nav.popBackStack() }) }
            }
            if (vm.busy && route != "scan") {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }

    if (showAdd) {
        val habitual = remember { vm.habitualBeers() }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Ajouter une bière") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (habitual.isNotEmpty()) {
                        Text("Encore une ?", style = MaterialTheme.typography.labelLarge, color = Amber)
                        habitual.forEach { b ->
                            OutlinedButton(
                                onClick = {
                                    showAdd = false
                                    scope.launch {
                                        vm.prepareAgain(b.id)
                                        nav.navigate("edit/tasting")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("🍺 ${b.name}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                        Text("Ou une autre :", style = MaterialTheme.typography.labelLarge, color = Amber)
                    }
                    Button(
                        onClick = { showAdd = false; nav.navigate("scan") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scanner le code-barres")
                    }
                    FilledTonalButton(
                        onClick = {
                            showAdd = false
                            val (uri, path) = vm.newPhoto()
                            pendingPhoto = path
                            takeLabelPhoto.launch(uri)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Photo de l'étiquette")
                    }
                    FilledTonalButton(
                        onClick = { showAdd = false; vm.startManual(); nav.navigate("choose") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Saisie manuelle")
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Annuler") } },
        )
    }
}
