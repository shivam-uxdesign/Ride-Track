package com.ridetrack.app.ui.bike

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ridetrack.app.ui.appViewModel
import com.ridetrack.app.ui.components.Label
import com.ridetrack.app.ui.components.PrimaryButton
import com.ridetrack.app.ui.components.ScreenHeader
import com.ridetrack.app.ui.theme.RtColors
import com.ridetrack.app.ui.theme.RtDimens
import com.ridetrack.app.ui.theme.RtType
import com.ridetrack.telemetry.model.FuelType
import com.ridetrack.telemetry.model.MountOrientation
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BikeEditScreen(bikeId: String?, onDone: () -> Unit, onCalibrate: (String) -> Unit) {
    val vm = appViewModel(key = "bike-edit-${bikeId ?: "new"}") { BikeEditViewModel(it, bikeId) }
    val f by vm.form.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = RtDimens.screenPadding),
    ) {
        ScreenHeader(if (vm.isNew) "Add bike" else "Edit bike", onBack = onDone)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Field("Make", f.make, "e.g. Bajaj", { v -> vm.update { it.copy(make = v) } }, capitalize = true)
            Field("Model", f.model, "e.g. Pulsar NS200", { v -> vm.update { it.copy(model = v) } }, capitalize = true)
            Row(horizontalArrangement = Arrangement.spacedBy(RtDimens.sm)) {
                Field("Year", f.year, "Optional", { v -> vm.update { it.copy(year = v.filter(Char::isDigit).take(4)) } }, Modifier.weight(1f), number = true, error = f.yearError)
                Field("Engine (cc)", f.displacementCc, "Optional", { v -> vm.update { it.copy(displacementCc = v.filter(Char::isDigit).take(4)) } }, Modifier.weight(1f), number = true, error = f.ccError)
            }
            Field("Weight (kg)", f.weightKg, "Optional", { v -> vm.update { it.copy(weightKg = v.filter(Char::isDigit).take(4)) } }, number = true, error = f.weightError)

            Spacer(Modifier.height(RtDimens.md))
            Label("Fuel type")
            Spacer(Modifier.height(RtDimens.xs))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(RtDimens.xs)) {
                FuelType.entries.forEach { t -> Choice(t.name.pretty(), f.fuelType == t) { vm.update { it.copy(fuelType = t) } } }
            }
            Spacer(Modifier.height(RtDimens.lg))
            Label("Phone mounting")
            Spacer(Modifier.height(RtDimens.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(RtDimens.xs)) {
                MountOrientation.entries.forEach { o -> Choice(o.name.pretty(), f.mountOrientation == o) { vm.update { it.copy(mountOrientation = o) } } }
            }
            Spacer(Modifier.height(RtDimens.xs))
            Text(
                "Mount the phone with its screen facing you. After saving, calibrate the mount so lean angle can be measured. Changing the mounting clears the calibration.",
                style = RtType.caption,
                color = RtColors.TextSecondary,
            )
            Spacer(Modifier.height(RtDimens.lg))
        }
        PrimaryButton(
            if (vm.isNew) "Save & calibrate" else "Save",
            onClick = { vm.save { id -> if (vm.isNew) onCalibrate(id) else onDone() } },
            enabled = f.isValid,
            large = true,
        )
        Spacer(Modifier.height(RtDimens.md))
    }
}

private fun String.pretty() = lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }

@Composable
private fun Field(
    label: String,
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    number: Boolean = false,
    capitalize: Boolean = false,
    error: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = RtColors.TextTertiary) },
        isError = error,
        supportingText = if (error) ({ Text("Check this value") }) else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (number) KeyboardType.Number else KeyboardType.Text,
            capitalization = if (capitalize) KeyboardCapitalization.Words else KeyboardCapitalization.None,
            imeAction = ImeAction.Next,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RtColors.Primary,
            unfocusedBorderColor = RtColors.Outline,
            focusedLabelColor = RtColors.Primary,
            cursorColor = RtColors.Primary,
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = RtDimens.xs),
    )
}

@Composable
private fun Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = RtColors.Primary.copy(alpha = 0.18f),
            selectedLabelColor = RtColors.Primary,
            labelColor = RtColors.TextSecondary,
        ),
    )
}
