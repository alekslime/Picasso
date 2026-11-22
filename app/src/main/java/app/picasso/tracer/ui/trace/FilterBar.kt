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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import app.picasso.tracer.domain.model.TraceFilter
import app.picasso.tracer.ui.theme.PicassoPaper
import app.picasso.tracer.ui.theme.PicassoPaperMuted

@Composable
fun FilterBar(
    selected: TraceFilter,
    onSelect: (TraceFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        PicassoSectionLabel(stringResource(R.string.filters).uppercase())
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChipItem(TraceFilter.ORIGINAL, R.string.filter_original, selected, onSelect)
            FilterChipItem(TraceFilter.GRAYSCALE, R.string.filter_grayscale, selected, onSelect)
            FilterChipItem(TraceFilter.HIGH_CONTRAST, R.string.filter_contrast, selected, onSelect)
            FilterChipItem(TraceFilter.EDGES, R.string.filter_edges, selected, onSelect)
            FilterChipItem(TraceFilter.POSTERIZE, R.string.filter_posterize, selected, onSelect)
        }
    }
}

@Composable
private fun FilterChipItem(
    filter: TraceFilter,
    labelRes: Int,
    selected: TraceFilter,
    onSelect: (TraceFilter) -> Unit,
) {
    PicassoFilterChip(
        selected = selected == filter,
        onClick = { onSelect(filter) },
        label = stringResource(labelRes),
    )
}

@Composable
fun GridBar(
    visible: Boolean,
    rows: Int,
    columns: Int,
    onToggle: () -> Unit,
    onRows: (Int) -> Unit,
    onColumns: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        PicassoFilterChip(
            selected = visible,
            onClick = onToggle,
            label = "${stringResource(R.string.grid)}  $rows × $columns",
        )
        if (visible) {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Stepper(
                    label = stringResource(R.string.grid_rows),
                    value = rows,
                    onValue = onRows,
                )
                Stepper(
                    label = stringResource(R.string.grid_columns),
                    value = columns,
                    onValue = onColumns,
                )
            }
        }
    }
}

@Composable
private fun Stepper(
    label: String,
    value: Int,
    onValue: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = PicassoPaperMuted,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onValue(value - 1) }) {
            Icon(Icons.Default.Remove, contentDescription = null, tint = PicassoPaper)
        }
        Text(
            "$value",
            style = MaterialTheme.typography.labelMedium,
            color = PicassoPaper,
            modifier = Modifier.widthIn(min = 20.dp),
        )
        IconButton(onClick = { onValue(value + 1) }) {
            Icon(Icons.Default.Add, contentDescription = null, tint = PicassoPaper)
        }
    }
}
