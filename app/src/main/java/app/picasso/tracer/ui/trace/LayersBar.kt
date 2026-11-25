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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
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
 * Picks which of a project's reference images the overlay shows: the base image
 * or one of the added layers. Layers are selected, never blended, share the
 * project's transform and filter, and each has its own opacity.
 */
@Composable
fun LayersBar(
    layers: List<OverlayLayer>,
    activeLayerId: Long?,
    busy: Boolean,
    onSelect: (Long?) -> Unit,
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
            PicassoFilterChip(
                selected = activeLayerId == null,
                onClick = { onSelect(null) },
                label = stringResource(R.string.layer_base),
            )
            layers.forEach { layer ->
                PicassoFilterChip(
                    selected = layer.id == activeLayerId,
                    onClick = { onSelect(layer.id) },
                    label = layer.name,
                )
            }
            PicassoFilterChip(
                selected = false,
                onClick = { if (!busy) onAdd() },
                label = stringResource(R.string.layer_add),
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
            )
            if (active != null) {
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
