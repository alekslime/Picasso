package app.picasso.tracer.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import app.picasso.tracer.domain.model.Project
import app.picasso.tracer.domain.model.Tags
import app.picasso.tracer.ui.trace.PicassoFilterChip

/**
 * Edits one project's tags: tap a tag to put it on or take it off, or type a new one. Tags that
 * end up on no project disappear by themselves.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagsDialog(
    project: Project,
    allTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    var selected by remember(project.id) { mutableStateOf(project.tags) }
    // Tags typed in this dialog are offered as chips too, even before they are saved anywhere.
    var added by remember(project.id) { mutableStateOf(emptyList<String>()) }
    var draft by remember(project.id) { mutableStateOf("") }
    val choices = remember(allTags, project.tags, added) {
        Tags.clean(allTags + project.tags + added.toList()).sortedBy { it.lowercase() }
    }

    fun isOn(tag: String) = selected.any { it.equals(tag, ignoreCase = true) }

    fun toggle(tag: String) {
        selected = if (isOn(tag)) {
            selected.filterNot { it.equals(tag, ignoreCase = true) }
        } else if (selected.size < Tags.MAX_PER_PROJECT) {
            selected + tag
        } else {
            selected
        }
    }

    fun addDraft() {
        val tag = Tags.normalize(draft) ?: return
        // An existing spelling wins, so "Faces" and "faces" stay one tag.
        val existing = choices.firstOrNull { it.equals(tag, ignoreCase = true) }
        if (existing == null) added = added + tag
        val name = existing ?: tag
        if (!isOn(name)) toggle(name)
        draft = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tags_title, project.name)) },
        text = {
            Column {
                if (choices.isEmpty()) {
                    Text(
                        stringResource(R.string.tags_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        choices.forEach { tag ->
                            PicassoFilterChip(selected = isOn(tag), onClick = { toggle(tag) }, label = tag)
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.take(Tags.MAX_LENGTH) },
                        placeholder = { Text(stringResource(R.string.tag_new_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = ::addDraft, enabled = Tags.normalize(draft) != null) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.tag_add))
                    }
                }
                if (selected.size >= Tags.MAX_PER_PROJECT) {
                    Text(
                        stringResource(R.string.tags_limit, Tags.MAX_PER_PROJECT),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selected) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
