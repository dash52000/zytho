@file:OptIn(ExperimentalMaterial3Api::class)

package fr.damien.zytho.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.damien.zytho.BuildConfigInfo
import fr.damien.zytho.ZythoViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: ZythoViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmRestore by remember { mutableStateOf(false) }
    val today = LocalDate.now().toString()

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            runCatching { vm.exportCsv(uri) }
                .onSuccess { toast("Export CSV terminé") }
                .onFailure { toast("Échec de l'export : ${it.message}") }
        }
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching { vm.backup(uri) }
                .onSuccess { toast("Sauvegarde enregistrée") }
                .onFailure { toast("Échec de la sauvegarde : ${it.message}") }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { vm.restore(uri) }
                .onSuccess { toast("Données restaurées") }
                .onFailure { toast("Restauration impossible : ${it.message}") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réglages et export") },
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
            SectionTitle("Analyse")
            Button(onClick = { csvLauncher.launch("zytho_degustations_$today.csv") }, modifier = Modifier.fillMaxWidth()) {
                Text("Exporter les dégustations (CSV)")
            }
            Text(
                "Une ligne par dégustation, séparateur virgule, UTF-8 : readr::read_csv() le lit tel quel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SectionTitle("Sauvegarde")
            FilledTonalButton(onClick = { backupLauncher.launch("zytho_sauvegarde_$today.json") }, modifier = Modifier.fillMaxWidth()) {
                Text("Sauvegarder toutes les données (JSON)")
            }
            OutlinedButton(onClick = { confirmRestore = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Restaurer une sauvegarde")
            }
            Text(
                "Les photos restent sur le téléphone : la sauvegarde contient les fiches, pas les images.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Zytho ${BuildConfigInfo.versionName(context)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Restaurer ?") },
            text = { Text("Les données actuelles seront remplacées par celles du fichier.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                }) { Text("Choisir le fichier") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Annuler") } },
        )
    }
}
