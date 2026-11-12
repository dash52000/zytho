@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package fr.damien.zytho.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import fr.damien.zytho.data.StyleGuide

/** Fiche pédagogique d'un style. */
@Composable
fun StyleScreen(index: Int, onBack: () -> Unit) {
    val s = StyleGuide.ALL.getOrNull(index)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (s == null) "Style" else "${s.emoji} ${s.name}") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                },
            )
        },
    ) { pad ->
        if (s == null) {
            Text("Style inconnu.", modifier = Modifier.padding(pad).padding(24.dp))
            return@Scaffold
        }
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(s.tagline, style = MaterialTheme.typography.titleMedium, color = Amber)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                if (s.robe.isNotBlank()) {
                    RobeSwatch(s.robe)
                    Spacer(Modifier.width(6.dp))
                    Text("${s.robe} · ", style = MaterialTheme.typography.bodySmall)
                }
                Text("${s.abv} · ${s.glass} · ${s.temp}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            AnselmeBubble(s.lesson)

            SectionTitle("Ce qu'on y cherche")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                s.expected.forEach { a ->
                    Text(
                        "${StyleGuide.icon(a)} ${a.lowercase()}",
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            ProfileBar("Douceur", s.sweet)
            ProfileBar("Amertume", s.bitter, Copper)
            ProfileBar("Corps", s.body)
            ProfileBar("Pétillance", s.fizz, Copper)

            SectionTitle("Pourquoi ça goûte ça")
            Text(s.why)
            SectionTitle("Bien servir")
            Text("${s.glass}, ${s.temp}. ${s.serve}")
            SectionTitle("Histoire & abbayes")
            Text(s.history)
            SectionTitle("Accords mets")
            Text(s.food)
            SectionTitle("À goûter")
            Text("Classique : ${s.classic}")
            Text("Près de chez toi : ${s.local ?: "pas de référence locale pour ce style"}")
            SectionTitle("Le savais-tu ?")
            s.facts.forEach { Text("• $it", modifier = Modifier.padding(bottom = 4.dp)) }
            Spacer(Modifier.height(40.dp))
        }
    }
}
