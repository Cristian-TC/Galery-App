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
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.galeryapp.ui.theme.GaleryAppTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GaleryAppTheme {
                GalleryApp()
            }
        }
    }

    @Composable
    fun GalleryApp() {
        val navController = rememberNavController()
        val context = LocalContext.current
        val permissionGranted = remember { mutableStateOf(false) }
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            permissionGranted.value = result.values.any { it }
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
            if (!granted) {
                permissionLauncher.launch(permissions)
            } else {
                permissionGranted.value = true
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!permissionGranted.value) {
                PermissionScreen(onRequestPermission = {
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                    } else {
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    permissionLauncher.launch(permissions)
                })
            } else {
                NavHost(navController = navController, startDestination = "gallery") {
                    composable("gallery") { GalleryMainScreen(navController) }
                    composable("vault") { VaultScreen(navController) }
                    composable("settings") { SettingsScreen(navController) }
                }
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

enum class SortType {
    DATE_NEWEST, DATE_OLDEST, NAME_ASC, NAME_DESC
}

enum class ViewType {
    GRID, COMPACT
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                .padding(24.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "Acceso a Galería",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Necesitamos permiso para mostrar tus fotos.\nTu privacidad es nuestra prioridad.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Permitir acceso", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryMainScreen(navController: NavController) {
    val context = LocalContext.current
    val photos = remember { mutableStateListOf<PhotoItem>() }
    val hiddenIds = remember { mutableStateListOf<Long>() }
    val favoriteIds = remember { mutableStateListOf<Long>() }
    val prefs = context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var currentSort by rememberSaveable { mutableStateOf(SortType.DATE_NEWEST) }
    var viewType by rememberSaveable { mutableStateOf(ViewType.GRID) }
    var selectedPhoto by remember { mutableStateOf<PhotoItem?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loadAllPhotos(context, photos, hiddenIds, favoriteIds, prefs)
    }

    val filteredAndSortedPhotos = remember(photos, hiddenIds, searchQuery, currentSort) {
        val query = searchQuery.trim().lowercase(Locale.getDefault())
        val filtered = photos.filter { photo ->
            val matchesSearch = query.isEmpty() || 
                photo.displayName.lowercase(Locale.getDefault()).contains(query)
            val isVisible = !hiddenIds.contains(photo.id)
            matchesSearch && isVisible
        }

        when (currentSort) {
            SortType.DATE_NEWEST -> filtered.sortedByDescending { it.timestamp }
            SortType.DATE_OLDEST -> filtered.sortedBy { it.timestamp }
            SortType.NAME_ASC -> filtered.sortedBy { it.displayName }
            SortType.NAME_DESC -> filtered.sortedByDescending { it.displayName }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Galería Profesional",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("vault") }) {
                        Icon(Icons.Default.Lock, contentDescription = "Bóveda")
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            PremiumSearchBar(searchQuery) { searchQuery = it }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Button(
                        onClick = { showSortMenu = !showSortMenu },
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ordenar", fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        SortType.entries.forEach { sort ->
                            DropdownMenuItem(
                                text = { Text(getSortLabel(sort)) },
                                onClick = { currentSort = sort; showSortMenu = false }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewType = ViewType.GRID },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (viewType == ViewType.GRID) 
                                MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { viewType = ViewType.COMPACT },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (viewType == ViewType.COMPACT) 
                                MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(Icons.Default.ViewList, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }

            StatsRow(filteredAndSortedPhotos.size, favoriteIds.size)

            if (filteredAndSortedPhotos.isEmpty()) {
                NoPhotosState()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(
                        minSize = if (viewType == ViewType.GRID) 140.dp else 120.dp
                    ),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredAndSortedPhotos, key = { it.id }) { photo ->
                        AdvancedPhotoCard(
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
                            onHide = {
                                hiddenIds.add(photo.id)
                                prefs.edit().putBoolean(photo.id.toString(), true).apply()
                                photos.remove(photo)
                            },
                            onOpen = { selectedPhoto = photo }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSearchBar(query: String, onQueryChanged: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp),
        placeholder = { Text("Buscar fotos...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant) },
        trailingIcon = if (query.isNotEmpty()) {
            { 
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent
        )
    )
}

@Composable
fun StatsRow(totalPhotos: Int, favoriteCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("Fotos", totalPhotos.toString(), Icons.Default.PhotoLibrary)
        Divider(modifier = Modifier.width(1.dp).height(40.dp), color = MaterialTheme.colorScheme.outlineVariant)
        StatItem("Favoritas", favoriteCount.toString(), Icons.Default.Favorite)
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun NoPhotosState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                .padding(24.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Sin fotos",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Tus fotos aparecerán aquí",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AdvancedPhotoCard(
    photo: PhotoItem,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onHide: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onOpen() }
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.aspectRatio(1f)) {
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
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f)
                            ),
                            startY = 200f
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Color.Black.copy(alpha = 0.4f),
                            shape = CircleShape
                        ),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (isFavorite) Color(0xFFFF6B6B) else Color.White
                    )
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onHide,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Color.Black.copy(alpha = 0.4f),
                            shape = CircleShape
                        ),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Text(
                photo.displayName,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            shape = RoundedCornerShape(0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = rememberAsyncImagePainter(photo.uri),
                    contentDescription = photo.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )

                TopBar(onClose = onClose)

                BottomActionBar(
                    photo = photo,
                    isFavorite = isFavorite,
                    onFavoriteToggle = onFavoriteToggle,
                    onShare = onShare
                )
            }
        }
    }
}

@Composable
fun TopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(48.dp)
                .background(
                    Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                )
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun BottomActionBar(
    photo: PhotoItem,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onShare: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) Color(0xFFFF6B6B) else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        photo.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Agregado: ${formatPhotoDate(photo.timestamp)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
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
        if (isAuthenticated) {
            loadHiddenPhotosForVault(context, hiddenPhotos, prefs)
        }
    }

    if (storedPin == null && !isAuthenticated) {
        PinSetupDialog(pin, { pin = it }, {
            if (pin.length >= 4) {
                prefs.edit().putString("pin", pin).apply()
                isAuthenticated = true
            } else {
                errorMessage = "El PIN debe tener al menos 4 dígitos"
            }
        }, { navController.popBackStack() }, errorMessage)
        return
    }

    if (storedPin != null && !isAuthenticated) {
        PinLoginDialog(pin, { pin = it }, {
            if (pin == storedPin) {
                isAuthenticated = true
                attemptCount = 0
            } else {
                attemptCount++
                errorMessage = if (attemptCount >= 3) "Muy muchos intentos. Intenta más tarde." else "PIN incorrecto"
            }
        }, { navController.popBackStack() }, errorMessage, attemptCount < 3)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bóveda Segura", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (hiddenPhotos.isEmpty()) {
            EmptyVaultState(modifier = Modifier.padding(padding))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(hiddenPhotos, key = { it.id }) { photo ->
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                hiddenPhotos.remove(photo)
                                prefs.edit().remove(photo.id.toString()).apply()
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Bóveda vacía", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Oculta tus fotos privadas aquí", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("EXPERIMENTAL_API_USAGE")
@Composable
fun SettingsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Aplicación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))
            ListItem(
                headlineContent = { Text("Versión") },
                supportingContent = { Text("1.0.0") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            ListItem(
                headlineContent = { Text("Galería Profesional") },
                supportingContent = { Text("Tu aplicación de fotos segura") },
                leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Sobre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))
            ListItem(
                headlineContent = { Text("Privacidad") },
                supportingContent = { Text("Tus fotos son tuyas") },
                leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) }
            )
            ListItem(
                headlineContent = { Text("Seguridad") },
                supportingContent = { Text("PIN protegido") },
                leadingContent = { Icon(Icons.Default.Security, contentDescription = null) }
            )
        }
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
        title = { Text("Configura tu PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { onPinChange(it.filter { c -> c.isDigit() }.take(6)) },
                    label = { Text("PIN (4-6 dígitos)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
        title = { Text("Bóveda Segura", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { onPinChange(it.filter { c -> c.isDigit() }.take(6)) },
                    label = { Text("Ingresa tu PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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

            val photo = PhotoItem(uri, id, name, timestamp, size)
            photos.add(photo)

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

fun formatPhotoDate(timestamp: Long): String {
    return if (timestamp > 0L) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    } else {
        "Fecha desconocida"
    }
}

fun getSortLabel(sort: SortType): String = when (sort) {
    SortType.DATE_NEWEST -> "📅 Más recientes"
    SortType.DATE_OLDEST -> "📅 Más antiguas"
    SortType.NAME_ASC -> "🔤 Nombre (A-Z)"
    SortType.NAME_DESC -> "🔤 Nombre (Z-A)"
}
