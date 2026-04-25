package com.example.galeryapp

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.galeryapp.ui.theme.GaleryAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GaleryAppTheme {
                GalleryApp()
            }
        }
    }
}

data class PhotoItem(
    val uri: Uri,
    val id: Long,
    val displayName: String,
    val timestamp: Long,
    val size: Long
)

enum class SortType { DATE_NEWEST, DATE_OLDEST, NAME_ASC, NAME_DESC, SIZE_DESC }
enum class ViewType { GRID, COMPACT }
enum class QuickFilter { ALL, FAVORITES, RECENT, LARGE }

enum class MainDestination(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Gallery("gallery", R.string.nav_gallery, Icons.Default.PhotoLibrary),
    Collections("collections", R.string.nav_collections, Icons.Default.Collections),
    Vault("vault", R.string.nav_vault, Icons.Default.Lock),
    Settings("settings", R.string.nav_settings, Icons.Default.Settings)
}

@Composable
fun GalleryApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionGranted = result.values.any { it }
    }

    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val granted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        permissionGranted = granted
        if (!granted) permissionLauncher.launch(permissions)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (!permissionGranted) {
            PermissionScreen {
                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                permissionLauncher.launch(permissions)
            }
        } else {
            val entry by navController.currentBackStackEntryAsState()
            val currentRoute = entry?.destination?.route
            val bottomBarRoutes = MainDestination.entries.map { it.route }
            Scaffold(
                bottomBar = {
                    if (currentRoute in bottomBarRoutes) {
                        BottomAppBar {
                            MainDestination.entries.forEach { item ->
                                NavigationBarItem(
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(MainDestination.Gallery.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(item.icon, contentDescription = null) },
                                    label = { Text(text = context.getString(item.titleRes), fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = MainDestination.Gallery.route,
                    modifier = Modifier.padding(padding)
                ) {
                    composable(MainDestination.Gallery.route) { GalleryMainScreen() }
                    composable(MainDestination.Collections.route) { CollectionsScreen() }
                    composable(MainDestination.Vault.route) { VaultScreen(navController) }
                    composable(MainDestination.Settings.route) { SettingsScreen() }
                }
            }
        }
    }
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ImageSearch,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .padding(24.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.permission_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.permission_message),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth(0.8f).height(52.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.permission_cta))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryMainScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
    val photos = remember { mutableStateListOf<PhotoItem>() }
    val hiddenIds = remember { mutableStateListOf<Long>() }
    val favoriteIds = remember { mutableStateListOf<Long>() }
    val selectedIds = remember { mutableStateListOf<Long>() }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var currentSort by rememberSaveable { mutableStateOf(SortType.DATE_NEWEST) }
    var viewType by rememberSaveable { mutableStateOf(ViewType.GRID) }
    var quickFilter by rememberSaveable { mutableStateOf(QuickFilter.ALL) }
    var selectedPhoto by remember { mutableStateOf<PhotoItem?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loadAllPhotos(context, photos, hiddenIds, favoriteIds, prefs)
    }

    val visiblePhotos by remember {
        derivedStateOf { photos.filter { !hiddenIds.contains(it.id) } }
    }
    val filteredAndSortedPhotos by remember(
        photos,
        hiddenIds,
        favoriteIds,
        searchQuery,
        currentSort,
        quickFilter
    ) {
        derivedStateOf {
        val query = searchQuery.trim().lowercase(Locale.getDefault())
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000

        visiblePhotos
            .filter { photo ->
                val byText = query.isBlank() || photo.displayName.lowercase(Locale.getDefault()).contains(query)
                val byFilter = when (quickFilter) {
                    QuickFilter.ALL -> true
                    QuickFilter.FAVORITES -> favoriteIds.contains(photo.id)
                    QuickFilter.RECENT -> photo.timestamp > 0L && now - photo.timestamp <= sevenDaysMs
                    QuickFilter.LARGE -> photo.size > 4_000_000L
                }
                byText && byFilter
            }
            .let { list ->
                when (currentSort) {
                    SortType.DATE_NEWEST -> list.sortedByDescending { it.timestamp }
                    SortType.DATE_OLDEST -> list.sortedBy { it.timestamp }
                    SortType.NAME_ASC -> list.sortedBy { it.displayName.lowercase(Locale.getDefault()) }
                    SortType.NAME_DESC -> list.sortedByDescending { it.displayName.lowercase(Locale.getDefault()) }
                    SortType.SIZE_DESC -> list.sortedByDescending { it.size }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (selectedIds.isEmpty()) stringResource(R.string.gallery_title) else "${selectedIds.size} seleccionadas",
                        fontWeight = FontWeight.Bold
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    AnimatedVisibility(visible = selectedIds.isEmpty(), enter = fadeIn(), exit = fadeOut()) {
                        Row {
                            IconButton(onClick = { showSortMenu = !showSortMenu }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Ordenar")
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                SortType.entries.forEach { sort ->
                                    DropdownMenuItem(
                                        text = { Text(getSortLabel(sort)) },
                                        onClick = {
                                            currentSort = sort
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                            IconButton(onClick = {
                                viewType = if (viewType == ViewType.GRID) ViewType.COMPACT else ViewType.GRID
                            }) {
                                Icon(
                                    imageVector = if (viewType == ViewType.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                    AnimatedVisibility(visible = selectedIds.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                        Row {
                            IconButton(onClick = {
                                selectedIds.forEach { id ->
                                    if (favoriteIds.contains(id)) {
                                        favoriteIds.remove(id)
                                        prefs.edit().remove("favorite_$id").apply()
                                    } else {
                                        favoriteIds.add(id)
                                        prefs.edit().putBoolean("favorite_$id", true).apply()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorita")
                            }
                            IconButton(onClick = {
                                val selectedUris = filteredAndSortedPhotos.filter { selectedIds.contains(it.id) }.map { it.uri }
                                if (selectedUris.isNotEmpty()) {
                                    sharePhotos(context, selectedUris)
                                }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Compartir")
                            }
                            IconButton(onClick = {
                                selectedIds.toList().forEach { id ->
                                    hiddenIds.add(id)
                                    prefs.edit().putBoolean(id.toString(), true).apply()
                                }
                                photos.removeAll { selectedIds.contains(it.id) }
                                selectedIds.clear()
                            }) {
                                Icon(Icons.Default.HideImage, contentDescription = "Ocultar")
                            }
                            IconButton(onClick = { selectedIds.clear() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancelar")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PremiumSearchBar(searchQuery) { searchQuery = it }
            DashboardSummary(
                totalPhotos = visiblePhotos.size,
                favorites = favoriteIds.count { id -> visiblePhotos.any { it.id == id } },
                hidden = hiddenIds.size,
                totalSizeBytes = visiblePhotos.sumOf { it.size }
            )
            QuickFilterChips(current = quickFilter, onSelect = { quickFilter = it })

            if (filteredAndSortedPhotos.isEmpty()) {
                NoPhotosState()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(if (viewType == ViewType.GRID) 138.dp else 112.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredAndSortedPhotos, key = { it.id }) { photo ->
                        AdvancedPhotoCard(
                            photo = photo,
                            isFavorite = favoriteIds.contains(photo.id),
                            isSelected = selectedIds.contains(photo.id),
                            onFavoriteToggle = {
                                if (favoriteIds.contains(photo.id)) {
                                    favoriteIds.remove(photo.id)
                                    prefs.edit().remove("favorite_${photo.id}").apply()
                                } else {
                                    favoriteIds.add(photo.id)
                                    prefs.edit().putBoolean("favorite_${photo.id}", true).apply()
                                }
                            },
                            onHide = {
                                hiddenIds.add(photo.id)
                                prefs.edit().putBoolean(photo.id.toString(), true).apply()
                                photos.remove(photo)
                            },
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    toggleSelection(selectedIds, photo.id)
                                } else {
                                    selectedPhoto = photo
                                }
                            },
                            onLongClick = {
                                toggleSelection(selectedIds, photo.id)
                            }
                        )
                    }
                }
            }
        }
    }

    selectedPhoto?.let { photo ->
        FullScreenPhotoViewerDialog(
            photo = photo,
            isFavorite = favoriteIds.contains(photo.id),
            onFavoriteToggle = {
                if (favoriteIds.contains(photo.id)) {
                    favoriteIds.remove(photo.id)
                    prefs.edit().remove("favorite_${photo.id}").apply()
                } else {
                    favoriteIds.add(photo.id)
                    prefs.edit().putBoolean("favorite_${photo.id}", true).apply()
                }
            },
            onClose = { selectedPhoto = null },
            onShare = { sharePhoto(context, photo.uri) }
        )
    }
}

private fun toggleSelection(selectedIds: MutableList<Long>, id: Long) {
    if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSearchBar(query: String, onQueryChanged: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Buscar por nombre...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Limpiar")
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    )
}

@Composable
fun DashboardSummary(totalPhotos: Int, favorites: Int, hidden: Int, totalSizeBytes: Long) {
    val estimatedGb = totalSizeBytes.toDouble() / (1024 * 1024 * 1024)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Panel inteligente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryMetric("Fotos", totalPhotos.toString())
                SummaryMetric("Favoritas", favorites.toString())
                SummaryMetric("Ocultas", hidden.toString())
                SummaryMetric("GB", String.format(Locale.US, "%.1f", estimatedGb))
            }
        }
    }
}

@Composable
fun SummaryMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun QuickFilterChips(current: QuickFilter, onSelect: (QuickFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickFilter.entries.forEach { filter ->
            val selected = current == filter
            AssistChip(
                onClick = { onSelect(filter) },
                label = { Text(text = getFilterLabel(filter)) },
                colors = if (selected) AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) else AssistChipDefaults.assistChipColors()
            )
        }
    }
}

@Composable
fun NoPhotosState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f))
                .padding(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Sin fotos para mostrar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Ajusta tus filtros, revisa la bóveda o agrega nuevas imágenes al dispositivo.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AdvancedPhotoCard(
    photo: PhotoItem,
    isFavorite: Boolean,
    isSelected: Boolean,
    onFavoriteToggle: () -> Unit,
    onHide: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.height(168.dp)) {
            Image(
                painter = rememberAsyncImagePainter(photo.uri),
                contentDescription = photo.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                            startY = 120f
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (isFavorite) Color(0xFFFF6B6B) else Color.White
                    )
                ) {
                    Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onHide,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    photo.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${formatPhotoDate(photo.timestamp)} • ${formatFileSize(photo.size)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun FullScreenPhotoViewerDialog(
    photo: PhotoItem,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClose: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = rememberAsyncImagePainter(photo.uri),
                    contentDescription = photo.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.74f))
                            )
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) Color(0xFFFF6B6B) else Color.White
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(photo.displayName, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(formatPhotoDate(photo.timestamp), color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun CollectionsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
    val photos = remember { mutableStateListOf<PhotoItem>() }
    val hiddenIds = remember { mutableStateListOf<Long>() }
    val favorites = remember { mutableStateListOf<Long>() }

    LaunchedEffect(Unit) {
        loadAllPhotos(context, photos, hiddenIds, favorites, prefs)
    }

    val grouped = photos
        .filter { !hiddenIds.contains(it.id) }
        .groupBy { groupPhotoByMonth(it.timestamp) }
        .toList()

    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Text("Colecciones", fontWeight = FontWeight.Bold) })
    }) { padding ->
        if (grouped.isEmpty()) {
            NoPhotosState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(grouped) { (label, photosInGroup) ->
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(photosInGroup.take(12), key = { it.id }) { item ->
                                    Image(
                                        painter = rememberAsyncImagePainter(item.uri),
                                        contentDescription = item.displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(112.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${photosInGroup.size} fotos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
    val storedPin = prefs.getString("pin", null)
    var pin by rememberSaveable { mutableStateOf("") }
    var isAuthenticated by remember { mutableStateOf(false) }
    val hiddenPhotos = remember { mutableStateListOf<PhotoItem>() }
    var errorMessage by remember { mutableStateOf("") }
    var attemptCount by remember { mutableStateOf(0) }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) loadHiddenPhotosForVault(context, hiddenPhotos, prefs)
    }

    if (storedPin == null && !isAuthenticated) {
        PinSetupDialog(
            pin = pin,
            onPinChange = { pin = it },
            onSave = {
                if (pin.length in 4..6) {
                    prefs.edit().putString("pin", pin).apply()
                    isAuthenticated = true
                } else {
                    errorMessage = "El PIN debe tener entre 4 y 6 dígitos"
                }
            },
            onCancel = { navController.navigate(MainDestination.Gallery.route) },
            errorMessage = errorMessage
        )
        return
    }

    if (storedPin != null && !isAuthenticated) {
        PinLoginDialog(
            pin = pin,
            onPinChange = { pin = it },
            onLogin = {
                if (pin == storedPin) {
                    isAuthenticated = true
                    errorMessage = ""
                    attemptCount = 0
                } else {
                    attemptCount++
                    errorMessage = if (attemptCount >= 3) "Demasiados intentos. Reinicia la app." else "PIN incorrecto"
                }
            },
            onCancel = { navController.navigate(MainDestination.Gallery.route) },
            errorMessage = errorMessage,
            isEnabled = attemptCount < 3
        )
        return
    }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Bóveda Segura") }) }) { padding ->
        if (hiddenPhotos.isEmpty()) {
            EmptyVaultState(modifier = Modifier.padding(padding))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(hiddenPhotos, key = { it.id }) { photo ->
                    Card(
                        modifier = Modifier
                            .height(145.dp)
                            .clickable {
                                hiddenPhotos.remove(photo)
                                prefs.edit().remove(photo.id.toString()).apply()
                            }
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(photo.uri),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyVaultState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Bóveda vacía", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Mantén pulsada una foto en la galería y toca ocultar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("EXPERIMENTAL_API_USAGE")
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
    var pinReset by remember { mutableStateOf(false) }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Configuración") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("General", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ListItem(
                headlineContent = { Text("Versión") },
                supportingContent = { Text("2.0.0 PRO") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            ListItem(
                headlineContent = { Text("Seguridad") },
                supportingContent = { Text("Bóveda protegida con PIN") },
                leadingContent = { Icon(Icons.Default.Security, contentDescription = null) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            Text("Gestión", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FilledTonalButton(onClick = { pinReset = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Restablecer PIN de bóveda")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Este reinicio solo borra el PIN local. Tus fotos no se eliminan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (pinReset) {
        AlertDialog(
            onDismissRequest = { pinReset = false },
            title = { Text("¿Restablecer PIN?") },
            text = { Text("Se solicitará un nuevo PIN la próxima vez que abras la bóveda.") },
            confirmButton = {
                Button(onClick = {
                    prefs.edit().remove("pin").apply()
                    pinReset = false
                }) {
                    Text("Restablecer")
                }
            },
            dismissButton = {
                TextButton(onClick = { pinReset = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun PinSetupDialog(
    pin: String,
    onPinChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    errorMessage: String
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Configura tu PIN") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { onPinChange(it.filter(Char::isDigit).take(6)) },
                    label = { Text("PIN (4-6 dígitos)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { Button(onClick = onSave) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } }
    )
}

@Composable
fun PinLoginDialog(
    pin: String,
    onPinChange: (String) -> Unit,
    onLogin: () -> Unit,
    onCancel: () -> Unit,
    errorMessage: String,
    isEnabled: Boolean
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Bóveda Segura") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { onPinChange(it.filter(Char::isDigit).take(6)) },
                    label = { Text("Ingresa tu PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = isEnabled
                )
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { Button(onClick = onLogin, enabled = isEnabled) { Text("Entrar") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } }
    )
}

fun loadAllPhotos(
    context: Context,
    photos: MutableList<PhotoItem>,
    hiddenIds: MutableList<Long>,
    favoriteIds: MutableList<Long>,
    prefs: android.content.SharedPreferences
) {
    photos.clear()
    hiddenIds.clear()
    favoriteIds.clear()

    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.SIZE
    )

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        "${MediaStore.Images.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn) ?: "Imagen"
            val timestamp = cursor.getLong(dateColumn).takeIf { it > 0 } ?: 0L
            val size = cursor.getLong(sizeColumn)
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            photos.add(PhotoItem(uri, id, name, timestamp, size))

            if (prefs.getBoolean(id.toString(), false)) hiddenIds.add(id)
            if (prefs.getBoolean("favorite_$id", false)) favoriteIds.add(id)
        }
    }
}

fun loadHiddenPhotosForVault(
    context: Context,
    hiddenPhotos: MutableList<PhotoItem>,
    prefs: android.content.SharedPreferences
) {
    hiddenPhotos.clear()
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.SIZE
    )

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        "${MediaStore.Images.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            if (prefs.getBoolean(id.toString(), false)) {
                val name = cursor.getString(nameColumn) ?: "Imagen"
                val timestamp = cursor.getLong(dateColumn).takeIf { it > 0 } ?: 0L
                val size = cursor.getLong(sizeColumn)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                hiddenPhotos.add(PhotoItem(uri, id, name, timestamp, size))
            }
        }
    }
}

fun sharePhoto(context: Context, uri: Uri) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Compartir foto"))
}

fun sharePhotos(context: Context, uris: List<Uri>) {
    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Compartir fotos"))
}

fun formatPhotoDate(timestamp: Long): String =
    if (timestamp > 0L) SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp)) else "Sin fecha"

fun formatFileSize(size: Long): String = when {
    size <= 0L -> "0 B"
    size < 1024L -> "$size B"
    size < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", size / 1024.0)
    size < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", size / (1024.0 * 1024.0))
    else -> String.format(Locale.US, "%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
}

fun groupPhotoByMonth(timestamp: Long): String {
    if (timestamp <= 0L) return "Sin fecha"
    return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(timestamp)).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}

fun getSortLabel(sort: SortType): String = when (sort) {
    SortType.DATE_NEWEST -> "📅 Más recientes"
    SortType.DATE_OLDEST -> "📅 Más antiguas"
    SortType.NAME_ASC -> "🔤 Nombre (A-Z)"
    SortType.NAME_DESC -> "🔤 Nombre (Z-A)"
    SortType.SIZE_DESC -> "📦 Más pesadas"
}

fun getFilterLabel(filter: QuickFilter): String = when (filter) {
    QuickFilter.ALL -> "Todas"
    QuickFilter.FAVORITES -> "Favoritas"
    QuickFilter.RECENT -> "Últimos 7 días"
    QuickFilter.LARGE -> "Archivos grandes"
}
