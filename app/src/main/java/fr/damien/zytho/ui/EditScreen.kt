@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package fr.damien.zytho.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.damien.zytho.ZythoViewModel
import fr.damien.zytho.data.Beers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun EditScreen(vm: ZythoViewModel, mode: String, onClose: () -> Unit) {
    var d by remember { mutableStateOf(vm.draft) }
    val aromas by vm.aromas.collectAsState()
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var newAroma by remember { mutableStateOf("") }
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val p = pendingPhoto
        if (ok && p != null) d = d.copy(photoPath = p)
    }

    val title = when (mode) {
        "tasting" -> "Dégustation"
        "cellar" -> "Ajout en cave"
        "wish" -> "Wishlist"
        else -> "Fiche bière"
    }

    fun save() {
        if (d.name.isBlank()) {
            error = "Donne au moins un nom à la bière"
            return
        }
        scope.launch {
            vm.save(d, mode)
            onClose()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                },
                actions = { TextButton(onClick = { save() }) { Text("Enregistrer", fontWeight = FontWeight.Bold) } },
            )
        },
    ) { pad ->
        val err = error
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            // ---------- La bière ----------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Thumb(d.photoPath, 72.dp)
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = {
                    val (uri, path) = vm.newPhoto()
                    pendingPhoto = path
                    takePhoto.launch(uri)
                }) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (d.photoPath == null) "Photo" else "Reprendre")
                }
            }
            OutlinedTextField(
                value = d.name, onValueChange = { d = d.copy(name = it); error = null },
                label = { Text("Nom de la bière") }, singleLine = true,
                isError = err != null,
                supportingText = if (err != null) { { Text(err) } } else null,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = d.breweryName, onValueChange = { d = d.copy(breweryName = it) },
                    label = { Text("Brasserie") }, singleLine = true, modifier = Modifier.weight(1.4f),
                )
                OutlinedTextField(
                    value = d.country, onValueChange = { d = d.copy(country = it) },
                    label = { Text("Pays") }, singleLine = true, modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = d.abv, onValueChange = { d = d.copy(abv = it) },
                    label = { Text("Degré %") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                Column(Modifier.weight(1.6f).padding(top = 8.dp)) {
                    ChoiceButton("Style", d.style, Beers.STYLES) { d = d.copy(style = it) }
                }
            }

            when (mode) {
                "tasting" -> TastingFields(
                    d = d,
                    aromaLabels = aromas.map { it.label },
                    newAroma = newAroma,
                    onNewAroma = { newAroma = it },
                    onAddAroma = {
                        val l = newAroma.trim()
                        if (l.isNotEmpty()) {
                            vm.addAroma(l)
                            d = d.copy(aromas = d.aromas + l)
                            newAroma = ""
                        }
                    },
                    onChange = { d = it },
                )
                "cellar" -> CellarFields(d, vm.suggestedDrinkBefore(d)) { d = it }
                "wish" -> {
                    SectionTitle("Pourquoi la goûter ?")
                    OutlinedTextField(
                        value = d.wishNote, onValueChange = { d = d.copy(wishNote = it) },
                        label = { Text("Note (où la trouver, qui l'a conseillée…)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    SectionTitle("À racheter ?")
                    ChipRow(Beers.REBUY, d.rebuy) { d = d.copy(rebuy = if (d.rebuy == it) "" else it) }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(onClick = { save() }, modifier = Modifier.fillMaxWidth()) { Text("Enregistrer") }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun TastingFields(
    d: fr.damien.zytho.Draft,
    aromaLabels: List<String>,
    newAroma: String,
    onNewAroma: (String) -> Unit,
    onAddAroma: () -> Unit,
    onChange: (fr.damien.zytho.Draft) -> Unit,
) {
    SectionTitle("Note")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = d.rating,
            onValueChange = { onChange(d.copy(rating = (it * 2).roundToInt() / 2f)) },
            valueRange = 0f..10f,
            steps = 19,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            Beers.fmtNote(d.rating.toDouble()) + " / 10",
            style = MaterialTheme.typography.titleLarge,
            color = Amber,
        )
    }

    SectionTitle("Arômes")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        aromaLabels.forEach { a ->
            val on = a in d.aromas
            FilterChip(
                selected = on,
                onClick = { onChange(d.copy(aromas = if (on) d.aromas - a else d.aromas + a)) },
                label = { Text(a) },
            )
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = newAroma, onValueChange = onNewAroma,
            label = { Text("Ajouter un arôme") }, singleLine = true,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onAddAroma) { Icon(Icons.Filled.Add, contentDescription = "Ajouter") }
    }

    SectionTitle("Contexte")
    ChipRow(Beers.PLACES, d.place) { onChange(d.copy(place = it)) }
    Spacer(Modifier.height(6.dp))
    ChipRow(Beers.SERVICES, d.service) { onChange(d.copy(service = it)) }
    Spacer(Modifier.height(6.dp))
    ChipRow(Beers.FORMATS, d.format) { onChange(d.copy(format = it)) }
    OutlinedTextField(
        value = d.vintage, onValueChange = { onChange(d.copy(vintage = it.filter { c -> c.isDigit() }.take(4))) },
        label = { Text("Millésime (optionnel)") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    )

    SectionTitle("Commentaire")
    OutlinedTextField(
        value = d.comment, onValueChange = { onChange(d.copy(comment = it)) },
        placeholder = { Text("Robe, mousse, finale, avec qui…") },
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )

    SectionTitle("À racheter ?")
    ChipRow(Beers.REBUY, d.rebuy) { onChange(d.copy(rebuy = if (d.rebuy == it) "" else it)) }
}

@Composable
private fun CellarFields(d: fr.damien.zytho.Draft, suggestion: Int, onChange: (fr.damien.zytho.Draft) -> Unit) {
    SectionTitle("Quantité")
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { if (d.quantity > 1) onChange(d.copy(quantity = d.quantity - 1)) }) {
            Icon(Icons.Filled.Remove, contentDescription = "Moins")
        }
        Text("${d.quantity}", style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = { onChange(d.copy(quantity = d.quantity + 1)) }) {
            Icon(Icons.Filled.Add, contentDescription = "Plus")
        }
    }
    SectionTitle("Format")
    ChipRow(Beers.FORMATS, d.format) { onChange(d.copy(format = it)) }
    OutlinedTextField(
        value = d.vintage, onValueChange = { onChange(d.copy(vintage = it.filter { c -> c.isDigit() }.take(4))) },
        label = { Text("Millésime") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    SectionTitle("À boire avant")
    OutlinedTextField(
        value = d.drinkBefore,
        onValueChange = { onChange(d.copy(drinkBefore = it.filter { c -> c.isDigit() }.take(4))) },
        label = { Text("Année limite") },
        placeholder = { Text("$suggestion") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    val style = d.style.ifBlank { "style non renseigné" }
    Text(
        "Conseil de garde : $suggestion ($style, ~${Beers.gardeYears(d.style)} an(s)). Laissé vide, ce conseil est appliqué.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}
