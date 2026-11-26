package app.picasso.tracer.ui.trace

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import app.picasso.tracer.domain.model.OverlayLayer
import kotlin.math.roundToInt

/**
 * A project's stack of reference images: the base image and the added layers. The selected
 * one is edited (placement, filter, opacity); the eye on each chip shows or hides it in the
 * stack, so several can be drawn at once. Chips run from the bottom of the stack to the top;
 * the arrows move the selected layer along it (the base always stays at the bottom).
 */
@Composable
fun LayersBar(
    layers: List<OverlayLayer>,
    activeLayerId: Long?,
    baseVisible: Boolean,
    busy: Boolean,
    onSelect: (Long?) -> Unit,
    onToggleVisible: (Long?) -> Unit,
    onMove: (Long, Int) -> Unit,
    onAdd: () -> Unit,
    onDelete: (OverlayLayer) -> Unit,
    onOpacity: (Long, Float) -> Unit,
    onRename: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf<OverlayLayer?>(null) }
    var renaming by remember { mutableStateOf<OverlayLayer?>(null) }
    val active = layers.firstOrNull { it.id == activeLayerId }

    Column(modifier = modifier) {
        PicassoSectionLabel(stringResource(R.string.layers).uppercase())
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LayerChip(
                label = stringResource(R.string.layer_base),
                selected = activeLayerId == null,
                visible = baseVisible,
                onSelect = { onSelect(null) },
                onToggleVisible = { onToggleVisible(null) },
            )
            layers.forEach { layer ->
                LayerChip(
                    label = layer.name,
                    selected = layer.id == activeLayerId,
                    visible = layer.visible,
                    onSelect = { onSelect(layer.id) },
                    onToggleVisible = { onToggleVisible(layer.id) },
                )
            }
            PicassoFilterChip(
                selected = false,
                onClick = { if (!busy) onAdd() },
                label = stringResource(R.string.layer_add),
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
            )
            if (active != null) {
                val position = layers.indexOfFirst { it.id == active.id }
                IconButton(onClick = { onMove(active.id, -1) }, enabled = position > 0) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.layer_send_back),
                    )
                }
                IconButton(onClick = { onMove(active.id, 1) }, enabled = position < layers.lastIndex) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.layer_bring_forward),
                    )
                }
                IconButton(onClick = { renaming = active }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.rename))
                }
                IconButton(onClick = { confirmDelete = active }) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.layer_delete),
                    )
                }
            }
        }
        if (active != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    stringResource(R.string.opacity),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(min = 72.dp),
                )
                PicassoSlider(
                    value = active.opacity,
                    onValueChange = { onOpacity(active.id, it) },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${(active.opacity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.widthIn(min = 40.dp),
                )
            }
        }
    }

    confirmDelete?.let { layer ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.layer_delete_title, layer.name)) },
            text = { Text(stringResource(R.string.layer_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    onDelete(layer)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    renaming?.let { layer ->
        RenameLayerDialog(
            layer = layer,
            onDismiss = { renaming = null },
            onConfirm = { name ->
                renaming = null
                onRename(layer.id, name)
            },
        )
    }
}

/** A layer's chip with its eye: tapping the chip selects it, the eye shows or hides it. */
@Composable
private fun LayerChip(
    label: String,
    selected: Boolean,
    visible: Boolean,
    onSelect: () -> Unit,
    onToggleVisible: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PicassoFilterChip(selected = selected, onClick = onSelect, label = label)
        IconButton(onClick = onToggleVisible) {
            Icon(
                if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = stringResource(if (visible) R.string.layer_hide else R.string.layer_show, label),
                tint = if (visible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RenameLayerDialog(
    layer: OverlayLayer,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(layer.id) { mutableStateOf(layer.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_layer)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.rename_layer_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.trim().isNotEmpty()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
