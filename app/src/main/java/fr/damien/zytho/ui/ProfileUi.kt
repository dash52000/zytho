package fr.damien.zytho.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.damien.zytho.ZythoViewModel

/** Pastille du profil actif : un appui pour changer, créer ou renommer un profil. */
@Composable
fun ProfileChip(vm: ZythoViewModel) {
    val profiles by vm.profiles.collectAsState()
    val activeId by vm.activeProfileId.collectAsState()
    val current = profiles.firstOrNull { it.id == activeId }
    val name = current?.name ?: "Moi"

    var open by remember { mutableStateOf(false) }
    /** "new", "rename" ou null */
    var dialog by remember { mutableStateOf<String?>(null) }

    Box {
        AssistChip(
            onClick = { open = true },
            label = { Text(name, maxLines = 1) },
            leadingIcon = { Initial(name) },
            colors = AssistChipDefaults.assistChipColors(labelColor = Amber),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            profiles.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.name) },
                    leadingIcon = {
                        if (p.id == activeId) Icon(Icons.Filled.Check, contentDescription = "Profil actif", tint = Amber)
                        else Initial(p.name)
                    },
                    onClick = { vm.switchProfile(p.id); open = false },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Nouveau profil…") }, onClick = { dialog = "new"; open = false })
            if (current != null) {
                DropdownMenuItem(text = { Text("Renommer « ${current.name} »…") }, onClick = { dialog = "rename"; open = false })
            }
        }
    }

    val mode = dialog
    if (mode != null) {
        var text by remember(mode) { mutableStateOf(if (mode == "rename") name else "") }
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(if (mode == "new") "Nouveau profil" else "Renommer le profil") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Prénom") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = text.isNotBlank(),
                    onClick = {
                        if (mode == "new") vm.addProfile(text)
                        else if (current != null) vm.renameProfile(current, text)
                        dialog = null
                    },
                ) { Text(if (mode == "new") "Créer" else "Renommer") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun Initial(name: String) {
    Box(
        Modifier.size(22.dp).clip(CircleShape).background(Amber),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().firstOrNull()?.uppercase() ?: "?",
            color = Color(0xFF2A1A0A),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}
