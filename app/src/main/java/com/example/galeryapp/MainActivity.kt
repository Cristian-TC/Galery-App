package com.example.galeryapp

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle permission result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GalleryApp()
        }
    }

    @Composable
    fun GalleryApp() {
        val navController = rememberNavController()
        val context = LocalContext.current

        // Request permission if needed
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }

        MaterialTheme {
            NavHost(navController = navController, startDestination = "gallery") {
                composable("gallery") { GalleryScreen(navController) }
                composable("vault") { VaultScreen(navController) }
            }
        }
    }
}

data class PhotoItem(val uri: Uri, val id: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(navController: NavController) {
    val context = LocalContext.current
    val photos = remember { mutableStateListOf<PhotoItem>() }
    val hiddenPhotos = remember { mutableStateListOf<PhotoItem>() }
    val prefs = context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)

    LaunchedEffect(Unit) {
        loadPhotos(context, photos, hiddenPhotos, prefs)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Galería") },
                actions = {
                    IconButton(onClick = { navController.navigate("vault") }) {
                        Icon(Icons.Filled.Lock, contentDescription = "Bóveda Oculta")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.padding(padding)
        ) {
            items(photos) { photo ->
                Card(
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                        .clickable {
                            // Toggle hide
                            if (hiddenPhotos.contains(photo)) {
                                hiddenPhotos.remove(photo)
                                prefs.edit().remove(photo.id.toString()).apply()
                            } else {
                                hiddenPhotos.add(photo)
                                prefs.edit().putBoolean(photo.id.toString(), true).apply()
                            }
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = rememberAsyncImagePainter(photo.uri),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (hiddenPhotos.contains(photo)) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "Oculto",
                                modifier = Modifier.align(Alignment.Center)
                            )
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
    var pin by remember { mutableStateOf("") }
    var isAuthenticated by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(true) }
    val storedPin = prefs.getString("pin", null)
    val hiddenPhotos = remember { mutableStateListOf<PhotoItem>() }

    if (storedPin == null) {
        // Set up PIN
        AlertDialog(
            onDismissRequest = { navController.popBackStack() },
            title = { Text("Configurar PIN") },
            text = {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (pin.isNotEmpty()) {
                        prefs.edit().putString("pin", pin).apply()
                        showDialog = false
                        isAuthenticated = true
                    }
                }) {
                    Text("Guardar")
                }
            }
        )
    } else if (!isAuthenticated) {
        AlertDialog(
            onDismissRequest = { navController.popBackStack() },
            title = { Text("Ingresar PIN") },
            text = {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (pin == storedPin) {
                        isAuthenticated = true
                        showDialog = false
                        loadHiddenPhotos(context, hiddenPhotos, prefs)
                    } else {
                        // Wrong PIN
                    }
                }) {
                    Text("Entrar")
                }
            }
        )
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Bóveda Oculta") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = "Volver")
                        }
                    }
                )
            }
        ) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(hiddenPhotos) { photo ->
                    Card(
                        modifier = Modifier
                            .padding(4.dp)
                            .aspectRatio(1f)
                            .clickable {
                                // Unhide
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

fun loadPhotos(context: Context, photos: MutableList<PhotoItem>, hiddenPhotos: MutableList<PhotoItem>, prefs: android.content.SharedPreferences) {
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            val photo = PhotoItem(uri, id)
            photos.add(photo)
            if (prefs.getBoolean(id.toString(), false)) {
                hiddenPhotos.add(photo)
            }
        }
    }
}

fun loadHiddenPhotos(context: Context, hiddenPhotos: MutableList<PhotoItem>, prefs: android.content.SharedPreferences) {
    val projection = arrayOf(MediaStore.Images.Media._ID)
    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        null
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            if (prefs.getBoolean(id.toString(), false)) {
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                hiddenPhotos.add(PhotoItem(uri, id))
            }
        }
    }
}
