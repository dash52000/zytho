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
import androidx.compose.runtime.mutableIntStateOf
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
import fr.damien.zytho.Draft
import fr.damien.zytho.ZythoViewModel
import fr.damien.zytho.data.Anselme
import fr.damien.zytho.data.Beers
import fr.damien.zytho.data.StyleGuide
import fr.damien.zytho.data.StyleInfo
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun EditScreen(vm: ZythoViewModel, mode: String, onClose: () -> Unit, onSavedTasting: (Long) -> Unit) {
    var d by remember { mutableStateOf(vm.draft) }
    val aromas by vm.aromas.collectAsState()
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = d.tastingId != null
    var showBeer by rememberSaveable { mutableStateOf(editing || d.beerId == null || d.name.isBlank()) }
    var guided by rememberSaveable {
        mutableStateOf(
            d.color.isNotBlank() || d.sweetness != null || d.bitterness != null ||
                d.body != null || d.fizz != null || d.finish.isNotBlank() ||
                d.aromas.any { it !in StyleGuide.FAMILY_LABEL.values }
        )
    }
    var step by rememberSaveable { mutableIntStateOf(0) }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val p = pendingPhoto
        if (ok && p != null) d = d.copy(photoPath = p)
    }

    val title = when (mode) {
        "tasting" -> if (editing) "Modifier la dégustation" else "Dégustation"
        "cellar" -> "Ajout en cave"
        "wish" -> "Wishlist"
        else -> "Fiche bière"
    }
    val style = StyleGuide.of(d.style)

    fun save() {
        if (d.name.isBlank()) {
            error = "Donne au moins un nom à la bière"
            showBeer = true
            return
        }
        scope.launch {
            vm.save(d, mode)
            if (mode == "tasting") onSavedTasting(vm.lastTastingId) else onClose()
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
                Thumb(d.photoPath, 56.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.name.ifBlank { "Nouvelle bière" }, style = MaterialTheme.typography.titleMedium)
                    val sub = listOf(d.breweryName, (style?.emoji ?: "") + " " + d.style)
                        .map { it.trim() }.filter { it.isNotBlank() }.joinToString(" · ")
                    if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { showBeer = !showBeer }) { Text(if (showBeer) "Masquer" else "Modifier") }
            }
            if (editing && d.source.isNotBlank()) Hint(d.source)
            if (showBeer || mode == "beer") {
                BeerFields(
                    d = d,
                    error = err,
                    onChange = { d = it; error = null },
                    onPhoto = {
                        val (uri, path) = vm.newPhoto()
                        pendingPhoto = path
                        takePhoto.launch(uri)
                    },
                )
            }

            when (mode) {
                "tasting" -> {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !guided, onClick = { guided = false }, label = { Text("⚡ Express") })
                        FilterChip(selected = guided, onClick = { guided = true }, label = { Text("🧭 Guidé par Anselme") })
                    }
                    if (guided) {
                        GuidedCards(
                            d = d, style = style, step = step,
                            aromaLabels = aromas.map { it.label },
                            customAromas = aromas.filter { it.custom }.map { it.label },
                            onStep = { step = it },
                            onAddAroma = { label -> vm.addAroma(label); d = d.copy(aromas = d.aromas + label) },
                            onChange = { d = it },
                            onSave = { save() },
                        )
                    } else {
                        ExpressFields(d = d, onChange = { d = it }, onGuided = { guided = true })
                    }
                }
                "cellar" -> CellarFields(d, vm.suggestedDrinkBefore(d)) { d = it }
                "wish" -> {
                    SectionTitle("Pourquoi la goûter ?")
                    OutlinedTextField(
                        value = d.wishNote, onValueChange = { d = d.copy(wishNote = it) },
                        label = { Text("Où la trouver, qui l'a conseillée…") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (style != null) {
                        Spacer(Modifier.height(12.dp))
                        AnselmeBubble("${style.emoji} ${style.name} : ${style.tagline}")
                    }
                }
                else -> {
                    SectionTitle("À racheter ?")
                    ChipRow(Beers.REBUY, d.rebuy) { d = d.copy(rebuy = if (d.rebuy == it) "" else it) }
                }
            }

            if (!(mode == "tasting" && guided)) {
                Spacer(Modifier.height(20.dp))
                Button(onClick = { save() }, modifier = Modifier.fillMaxWidth()) { Text("Enregistrer") }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun BeerFields(d: Draft, error: String?, onChange: (Draft) -> Unit, onPhoto: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        OutlinedButton(onClick = onPhoto) {
            Icon(Icons.Filled.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (d.photoPath == null) "Photo" else "Reprendre la photo")
        }
    }
    OutlinedTextField(
        value = d.name, onValueChange = { onChange(d.copy(name = it)) },
        label = { Text("Nom de la bière") }, singleLine = true,
        isError = error != null,
        supportingText = if (error != null) { { Text(error) } } else null,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = d.breweryName, onValueChange = { onChange(d.copy(breweryName = it)) },
            label = { Text("Brasserie") }, singleLine = true, modifier = Modifier.weight(1.4f),
        )
        OutlinedTextField(
            value = d.country, onValueChange = { onChange(d.copy(country = it)) },
            label = { Text("Pays") }, singleLine = true, modifier = Modifier.weight(1f),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = d.abv, onValueChange = { onChange(d.copy(abv = it)) },
            label = { Text("Degré %") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        Column(Modifier.weight(1.6f).padding(top = 8.dp)) {
            ChoiceButton("Style", d.style, Beers.STYLES) { onChange(d.copy(style = it)) }
        }
    }
}

@Composable
private fun RatingSlider(d: Draft, onChange: (Draft) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = d.rating,
            onValueChange = { onChange(d.copy(rating = (it * 2).roundToInt() / 2f)) },
            valueRange = 0f..10f,
            steps = 19,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(Beers.fmtNote(d.rating.toDouble()) + " / 10", style = MaterialTheme.typography.titleLarge, color = Amber)
    }
}

@Composable
private fun ExpressFields(d: Draft, onChange: (Draft) -> Unit, onGuided: () -> Unit) {
    var showContext by rememberSaveable { mutableStateOf(false) }
    SectionTitle("Ta note")
    RatingSlider(d, onChange)
    SectionTitle("Ce que tu sens (facultatif)")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StyleGuide.FAMILIES.filter { it.first != "Perso" }.forEach { (fam, icon, _) ->
            val label = StyleGuide.FAMILY_LABEL[fam] ?: fam
            val on = label in d.aromas
            FilterChip(
                selected = on,
                onClick = { onChange(d.copy(aromas = if (on) d.aromas - label else d.aromas + label)) },
                label = { Text("$icon $label") },
            )
        }
    }
    OutlinedTextField(
        value = d.comment, onValueChange = { onChange(d.copy(comment = it)) },
        label = { Text("Un mot ? (facultatif)") },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    TextButton(onClick = { showContext = !showContext }) {
        Text(if (showContext) "Masquer le contexte" else "Lieu, service, format… (${d.place}, ${d.service.lowercase()}, ${d.format})")
    }
    if (showContext) ContextFields(d, onChange)
    TextButton(onClick = onGuided) { Text("Envie d'aller plus loin ? Frère Anselme peut te guider →") }
}

@Composable
private fun ContextFields(d: Draft, onChange: (Draft) -> Unit) {
    ChipRow(Beers.PLACES, d.place) { onChange(d.copy(place = it)) }
    Spacer(Modifier.height(6.dp))
    ChipRow(Beers.SERVICES, d.service) { onChange(d.copy(service = it)) }
    Spacer(Modifier.height(6.dp))
    ChipRow(Beers.FORMATS, d.format) { onChange(d.copy(format = it)) }
    OutlinedTextField(
        value = d.vintage, onValueChange = { onChange(d.copy(vintage = it.filter { c -> c.isDigit() }.take(4))) },
        label = { Text("Millésime (facultatif)") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    )
}

private val STEPS = listOf("👁 Œil", "👃 Nez", "👅 Bouche", "⏳ Finale")

@Composable
private fun GuidedCards(
    d: Draft,
    style: StyleInfo?,
    step: Int,
    aromaLabels: List<String>,
    customAromas: List<String>,
    onStep: (Int) -> Unit,
    onAddAroma: (String) -> Unit,
    onChange: (Draft) -> Unit,
    onSave: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        STEPS.forEachIndexed { i, s ->
            FilterChip(selected = step == i, onClick = { onStep(i) }, label = { Text(s) })
        }
    }
    Spacer(Modifier.height(8.dp))
    when (step) {
        0 -> {
            AnselmeBubble(Anselme.tipEye(style))
            SectionTitle("La robe")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StyleGuide.ROBES.forEach { (robe, _) ->
                    FilterChip(
                        selected = d.color == robe,
                        onClick = { onChange(d.copy(color = if (d.color == robe) "" else robe)) },
                        label = { Text(robe) },
                        leadingIcon = { RobeSwatch(robe) },
                    )
                }
            }
            if (style != null && style.robe.isNotBlank()) Hint("Attendu pour une ${style.name} : ${style.robe.lowercase()}.")
        }
        1 -> NoseCard(d, style, aromaLabels, customAromas, onAddAroma, onChange)
        2 -> {
            AnselmeBubble(Anselme.tipMouth(style))
            SectionTitle("En bouche")
            ProfileSlider("Douceur", listOf("Très sèche", "Sèche", "Équilibrée", "Douce", "Très douce"), d.sweetness) { onChange(d.copy(sweetness = it)) }
            ProfileSlider("Amertume", listOf("Aucune", "Légère", "Moyenne", "Marquée", "Intense"), d.bitterness) { onChange(d.copy(bitterness = it)) }
            ProfileSlider("Corps", listOf("Aqueux", "Léger", "Moyen", "Rond", "Épais"), d.body) { onChange(d.copy(body = it)) }
            ProfileSlider("Pétillance", listOf("Plate", "Discrète", "Moyenne", "Vive", "Explosive"), d.fizz) { onChange(d.copy(fizz = it)) }
        }
        else -> {
            AnselmeBubble(Anselme.tipFinish(style))
            SectionTitle("La finale")
            ChipRow(listOf("Courte", "Moyenne", "Longue"), d.finish) { onChange(d.copy(finish = if (d.finish == it) "" else it)) }
            SectionTitle("Ta note")
            RatingSlider(d, onChange)
            OutlinedTextField(
                value = d.comment, onValueChange = { onChange(d.copy(comment = it)) },
                label = { Text("Ton impression, avec qui, où… (facultatif)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            SectionTitle("À racheter ?")
            ChipRow(Beers.REBUY, d.rebuy) { onChange(d.copy(rebuy = if (d.rebuy == it) "" else it)) }
            SectionTitle("Contexte")
            ContextFields(d, onChange)
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (step > 0) OutlinedButton(onClick = { onStep(step - 1) }) { Text("← Précédent") }
        Spacer(Modifier.weight(1f))
        if (step < STEPS.size - 1) {
            Button(onClick = { onStep(step + 1) }) { Text("Suivant →") }
        } else {
            Button(onClick = onSave) { Text("Enregistrer") }
        }
    }
    Hint("Tu peux enregistrer à tout moment : rien n'est obligatoire, sauf la note.")
}

@Composable
private fun NoseCard(
    d: Draft,
    style: StyleInfo?,
    aromaLabels: List<String>,
    customAromas: List<String>,
    onAddAroma: (String) -> Unit,
    onChange: (Draft) -> Unit,
) {
    var family by rememberSaveable { mutableStateOf(style?.expected?.firstOrNull()?.let { Anselme.familyOf(it) } ?: "Fruits") }
    var newAroma by remember { mutableStateOf("") }

    AnselmeBubble(Anselme.tipNose(style))
    SectionTitle("1. Une grande famille ?")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StyleGuide.FAMILIES.forEach { (fam, icon, _) ->
            if (fam != "Perso" || customAromas.isNotEmpty()) {
                FilterChip(
                    selected = family == fam,
                    onClick = { family = fam },
                    label = { Text("$icon ${StyleGuide.FAMILY_LABEL[fam] ?: fam}") },
                )
            }
        }
    }
    val comparison = StyleGuide.FAMILIES.firstOrNull { it.first == family }?.third
    if (comparison != null) Hint(comparison)

    SectionTitle("2. Plus précisément ?")
    val inFamily = if (family == "Perso") customAromas
    else aromaLabels.filter { Anselme.familyOf(it) == family }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        inFamily.forEach { a ->
            val on = a in d.aromas
            val star = if (style != null && a in style.expected) " ★" else ""
            FilterChip(
                selected = on,
                onClick = { onChange(d.copy(aromas = if (on) d.aromas - a else d.aromas + a)) },
                label = { Text("${StyleGuide.icon(a)} $a$star") },
            )
        }
    }
    if (style != null) Hint("★ = attendu dans une ${style.name}")
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = newAroma, onValueChange = { newAroma = it },
            label = { Text("Un arôme à toi ?") }, singleLine = true,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = {
            val l = newAroma.trim()
            if (l.isNotEmpty()) {
                onAddAroma(l)
                newAroma = ""
            }
        }) { Icon(Icons.Filled.Add, contentDescription = "Ajouter") }
    }
    if (d.aromas.isNotEmpty()) {
        Text(
            "Ta sélection : " + d.aromas.joinToString(" ") { StyleGuide.icon(it) + " " + it.lowercase() },
            style = MaterialTheme.typography.bodyMedium,
            color = Amber,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun ProfileSlider(label: String, labels: List<String>, value: Int?, onChange: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                if (value == null) "à toi de dire" else labels[value],
                color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else Amber,
            )
        }
        Slider(
            value = (value ?: 2).toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(0, 4)) },
            valueRange = 0f..4f,
            steps = 3,
        )
    }
}

@Composable
private fun CellarFields(d: Draft, suggestion: Int, onChange: (Draft) -> Unit) {
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
    Spacer(Modifier.height(8.dp))
    AnselmeBubble("Pour ce style ($style), je conseille environ ${Beers.gardeYears(d.style)} an(s) de garde : à boire avant $suggestion. Laisse le champ vide pour suivre mon conseil.")
}
