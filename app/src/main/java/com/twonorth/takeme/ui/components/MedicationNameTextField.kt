package com.twonorth.takeme.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.twonorth.takeme.data.CommonMedicationNames

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationNameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    val suggestions = remember(value) {
        if (value.isBlank()) {
            emptyList()
        } else {
            val lowerValue = value.lowercase()
            // Filter all names that contain the typed text
            val matches = CommonMedicationNames.NAMES.filter { 
                it.contains(lowerValue, ignoreCase = true)
            }
            
            // Rank: startsWith first, then contains
            val (startsWith, contains) = matches.partition { 
                it.startsWith(lowerValue, ignoreCase = true)
            }
            
            // Sort and take top 6
            (startsWith.sortedBy { it.lowercase() } + contains.sortedBy { it.lowercase() })
                .distinct()
                .take(6)
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && suggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        if (suggestions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            onValueChange(suggestion)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}
