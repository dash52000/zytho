package fr.damien.zytho.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.damien.zytho.ZythoViewModel
import fr.damien.zytho.data.Beers
import fr.damien.zytho.data.Prescription
import fr.damien.zytho.data.Reco

/** Les conseils d'Anselme, sous la carte du goût. */
@Composable
fun RecoSection(
    vm: ZythoViewModel,
    prescription: Prescription,
    points: List<Pair<Float, Float>>,
    center: Pair<Float, Float>?,
) {
    SectionTitle("Anselme te conseille")
    if (prescription.intro.isNotEmpty()) {
        AnselmeBubble(prescription.intro)
    }
    val context = LocalContext.current
    prescription.recos.forEach { reco ->
        RecoCard(
            reco,
            onWish = {
                vm.addRecoToWish(reco)
                Toast.makeText(context, "${reco.candidate.name} ajoutée à ta wishlist", Toast.LENGTH_SHORT).show()
            },
            onOther = { vm.otherIdea(reco.kind, prescription.recos, points, center) },
            onNotForMe = { vm.notForMe(reco) },
        )
    }
    if (prescription.recos.isNotEmpty()) {
        Hint("Points pâles sur la carte : les bières de mon catalogue que tu n'as pas encore goûtées.")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecoCard(reco: Reco, onWish: () -> Unit, onOther: () -> Unit, onNotForMe: () -> Unit) {
    val c = reco.candidate
    Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "${reco.kind.emoji} ${reco.kind.title}",
                style = MaterialTheme.typography.labelLarge,
                color = Amber,
                fontWeight = FontWeight.Bold,
            )
            Text(c.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 2.dp))
            val sub = listOfNotNull(
                c.brewery.ifBlank { null },
                c.style,
                Beers.fmtAbv(c.abv).ifBlank { null }?.let { "$it %" },
                if (c.trappist) "✝ Trappiste" else null,
            ).joinToString(" · ")
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            reco.why.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                FilledTonalButton(onClick = onWish) { Text("→ Wishlist") }
                if (reco.hasAlternative) TextButton(onClick = onOther) { Text("Autre idée") }
                TextButton(onClick = onNotForMe) { Text("Pas pour moi") }
            }
        }
    }
}
