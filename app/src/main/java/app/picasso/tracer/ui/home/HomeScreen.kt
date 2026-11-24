package app.picasso.tracer.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import app.picasso.tracer.R
import app.picasso.tracer.domain.model.Project
import coil.compose.AsyncImage
import java.io.File
import java.text.DateFormat
import java.util.Date
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenProject: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsState()
    val library by viewModel.library.collectAsState()
    val samples by viewModel.samples.collectAsState()
    val creating by viewModel.creating.collectAsState()
    val errorRes by viewModel.errorRes.collectAsState()
    val query by viewModel.query.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val favoritesOnly by viewModel.favoritesOnly.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val showAds by viewModel.showAds.collectAsState()
    val activity = context as? Activity
    LaunchedEffect(Unit) { activity?.let { viewModel.ads.ensureConsent(it) } }
    val untitled = stringResource(R.string.project_untitled)

    var deleting by remember { mutableStateOf<Project?>(null) }
    var renaming by remember { mutableStateOf<Project?>(null) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) viewModel.createProject(uri, untitled, onOpenProject)
    }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved: Boolean ->
        val uri = photoUri
        if (saved && uri != null) viewModel.createProject(uri, untitled, onOpenProject)
    }

    val errorMessage = errorRes?.let { stringResource(it) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbar.showSnackbar(errorMessage)
            viewModel.clearError()
        }
    }

    // Resume only makes sense outside search/favorites filtering.
    val browsing = query.isBlank() && !favoritesOnly
    val resume = if (browsing) projects.firstOrNull() else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.home_title))
                        Text(
                            stringResource(R.string.tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showAds) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    AdBanner(
                        visible = true,
                        adRequest = viewModel.ads::adRequest,
                    )
                }
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().entrance(0)) {
                    NewTracingRow(
                        creating = creating,
                        onPick = {
                            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onCapture = {
                            val file = File(
                                File(context.cacheDir, "images").apply { mkdirs() },
                                "capture-${System.currentTimeMillis()}.jpg",
                            )
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file,
                            )
                            photoUri = uri
                            takePhoto.launch(uri)
                        },
                    )
                }
            }
            if (creating) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            resume?.let { project ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ResumeCard(project = project, onOpen = { onOpenProject(project.id) }, modifier = Modifier.entrance(1))
                }
            }
            if (samples.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(R.string.try_sample),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp).entrance(2),
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.entrance(3)) {
                        items(samples) { item ->
                            SampleCard(
                                item = item,
                                enabled = !creating,
                                onSelect = {
                                    viewModel.createSample(
                                        item.sample,
                                        context.getString(item.sample.nameRes),
                                        onOpenProject,
                                    )
                                },
                            )
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .entrance(4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(R.string.library),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (library.isNotEmpty()) {
                        Text(
                            "${library.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (projects.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.entrance(5),
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = viewModel::setQuery,
                            placeholder = { Text(stringResource(R.string.search_projects)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                        )
                        SortMenu(sort = sort, onSort = viewModel::setSort)
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.entrance(6)) {
                        FilterChip(
                            selected = !favoritesOnly,
                            onClick = { viewModel.setFavoritesOnly(false) },
                            label = { Text(stringResource(R.string.filter_all)) },
                        )
                        FilterChip(
                            selected = favoritesOnly,
                            onClick = { viewModel.setFavoritesOnly(true) },
                            label = { Text(stringResource(R.string.filter_favorites)) },
                        )
                    }
                }
            }
            itemsIndexed(library, key = { _, p -> p.id }) { index, project ->
                ProjectCard(
                    // Cards glide to their new slot on delete, sort and filter. Fade-in is off because entrance() does it.
                    modifier = Modifier.animateItem(fadeInSpec = null).entrance(7 + index),
                    project = project,
                    onOpen = { onOpenProject(project.id) },
                    onToggleFavorite = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleFavorite(project)
                    },
                    onRename = { renaming = project },
                    onDelete = { deleting = project },
                )
            }
            if (library.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    when {
                        projects.isEmpty() -> EmptyProjects()
                        favoritesOnly -> EmptyHint(stringResource(R.string.no_favorites))
                        else -> EmptyHint(stringResource(R.string.no_matches))
                    }
                }
            }
        }
    }

    deleting?.let { project ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.delete_project)) },
            text = { Text(stringResource(R.string.delete_project_confirm)) },
            confirmButton = {
                TextButton(onClick = { deleting = null; viewModel.deleteProject(project) }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    renaming?.let { project ->
        RenameDialog(
            project = project,
            onDismiss = { renaming = null },
            onConfirm = { name ->
                renaming = null
                viewModel.renameProject(project, name)
            },
        )
    }
}

@Composable
private fun NewTracingRow(
    creating: Boolean,
    onPick: () -> Unit,
    onCapture: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onPick,
            modifier = Modifier.weight(1f).height(52.dp),
            enabled = !creating,
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.pick_image))
        }
        FilledTonalButton(
            onClick = onCapture,
            modifier = Modifier.weight(1f).height(52.dp),
            enabled = !creating,
        ) {
            Icon(Icons.Default.AddAPhoto, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.take_photo))
        }
    }
}

@Composable
private fun ResumeCard(
    project: Project,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val press = remember { MutableInteractionSource() }
    Card(
        onClick = onOpen,
        interactionSource = press,
        modifier = modifier.pressScale(press),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = File(project.imagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.continue_last).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(project.updatedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SampleCard(
    item: SampleItem,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    val press = remember { MutableInteractionSource() }
    Card(
        onClick = onSelect,
        enabled = enabled,
        interactionSource = press,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.width(140.dp).pressScale(press),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            AsyncImage(
                model = item.file,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(item.sample.nameRes),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SortMenu(
    sort: LibrarySort,
    onSort: (LibrarySort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Sort, contentDescription = stringResource(R.string.sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOption(
                label = stringResource(R.string.sort_newest),
                selected = sort == LibrarySort.NEWEST,
                onClick = { expanded = false; onSort(LibrarySort.NEWEST) },
            )
            SortOption(
                label = stringResource(R.string.sort_oldest),
                selected = sort == LibrarySort.OLDEST,
                onClick = { expanded = false; onSort(LibrarySort.OLDEST) },
            )
            SortOption(
                label = stringResource(R.string.sort_name),
                selected = sort == LibrarySort.NAME,
                onClick = { expanded = false; onSort(LibrarySort.NAME) },
            )
        }
    }
}

@Composable
private fun SortOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null) }
        } else null,
        onClick = onClick,
    )
}

@Composable
private fun ProjectCard(
    project: Project,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val press = remember { MutableInteractionSource() }
    Card(
        onClick = onOpen,
        interactionSource = press,
        modifier = modifier.pressScale(press),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column {
            AsyncImage(
                model = File(project.imagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        project.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        RelativeTime(project.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FavoriteStar(favorite = project.favorite, onClick = onToggleFavorite)
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.project_options))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rename)) },
                            onClick = { menuExpanded = false; onRename() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = { menuExpanded = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}

/** Star that pops when favorited, dips when unfavorited, and fades its colour either way. */
@Composable
private fun FavoriteStar(favorite: Boolean, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    var firstRun by remember { mutableStateOf(true) }
    LaunchedEffect(favorite) {
        if (firstRun) {
            firstRun = false
            return@LaunchedEffect
        }
        if (favorite) {
            scale.snapTo(0.6f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        } else {
            scale.animateTo(0.82f, tween(80))
            scale.animateTo(1f, tween(140))
        }
    }
    val tint by animateColorAsState(
        targetValue = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "favoriteTint",
    )
    IconButton(onClick = onClick) {
        Icon(
            if (favorite) Icons.Default.Star else Icons.Default.StarBorder,
            contentDescription = stringResource(R.string.toggle_favorite),
            tint = tint,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        )
    }
}

/** Library cards show relative times instead of the full timestamp. */
@Composable
private fun RelativeTime(millis: Long): String {
    val seconds = (System.currentTimeMillis() - millis) / 1000
    return when {
        seconds < 60 -> stringResource(R.string.rel_seconds_ago, seconds.coerceAtLeast(0))
        seconds < 3600 -> stringResource(R.string.rel_minutes_ago, seconds / 60)
        seconds < 86_400 -> stringResource(R.string.rel_hours_ago, seconds / 3600)
        else -> {
            val days = seconds / 86_400
            when {
                days < 2 -> stringResource(if (days == 1L) R.string.rel_yesterday else R.string.rel_today)
                days < 30 -> stringResource(R.string.rel_days_ago, days)
                else -> DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(millis))
            }
        }
    }
}

@Composable
private fun RenameDialog(
    project: Project,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(project.id) { mutableStateOf(project.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_project)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.rename_hint)) },
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun EmptyHint(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
    )
}

@Composable
private fun EmptyProjects() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.no_projects),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Fades and lifts a home item into place once, staggered by [index]. The flag is saved, so scrolling an
 * item back into view or returning from another screen doesn't replay it.
 */
@Composable
private fun Modifier.entrance(index: Int): Modifier {
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            delay(index.coerceAtMost(10) * 45L)
            progress.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
            played = true
        }
    }
    val lift = with(LocalDensity.current) { 16.dp.toPx() }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * lift
    }
}

/** Shrinks slightly while pressed and springs back on release. */
@Composable
private fun Modifier.pressScale(source: InteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
