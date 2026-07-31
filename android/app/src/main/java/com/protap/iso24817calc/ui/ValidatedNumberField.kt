package com.protap.iso24817calc.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun ValidatedNumberField(
    field: FieldId,
    label: String,
    unit: String,
    value: String,
    error: String?,
    integer: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { next -> onValueChange(next.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }) },
        label = { Text(label) },
        suffix = if (unit.isNotBlank()) ({ Text(unit) }) else null,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}
