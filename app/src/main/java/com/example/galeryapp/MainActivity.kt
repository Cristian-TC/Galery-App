package com.example.galeryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                MultinationalGalleryScreen()
            }
        }
    }
}

data class GalleryItem(
    val country: String,
    val city: String,
    val location: String,
    val region: Region,
    val yearlyVisitors: String,
    val imageUrl: String,
    val author: String
)

enum class Region {
    EUROPE, AMERICAS, ASIA, AFRICA, OCEANIA
}

private val galleryItems = listOf(
    GalleryItem("España", "Barcelona", "Sagrada Familia", Region.EUROPE, "4.7 M", "https://images.unsplash.com/photo-1583422409516-2895a77efded?auto=format&fit=crop&w=1000&q=80", "Nicolas Vigier"),
    GalleryItem("Francia", "París", "Torre Eiffel", Region.EUROPE, "6.2 M", "https://images.unsplash.com/photo-1431274172761-fca41d930114?auto=format&fit=crop&w=1000&q=80", "Pedro Lastra"),
    GalleryItem("México", "Chichén Itzá", "Pirámide de Kukulkán", Region.AMERICAS, "2.6 M", "https://images.unsplash.com/photo-1585464231875-d9ef1f5ad396?auto=format&fit=crop&w=1000&q=80", "Bhargava Marripati"),
    GalleryItem("Estados Unidos", "Nueva York", "Puente de Brooklyn", Region.AMERICAS, "30 M", "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?auto=format&fit=crop&w=1000&q=80", "Luca Bravo"),
    GalleryItem("Japón", "Kioto", "Fushimi Inari", Region.ASIA, "3.0 M", "https://images.unsplash.com/photo-1545569341-9eb8b30979d9?auto=format&fit=crop&w=1000&q=80", "Sorasak"),
    GalleryItem("India", "Agra", "Taj Mahal", Region.ASIA, "7.5 M", "https://images.unsplash.com/photo-1564507592333-c60657eea523?auto=format&fit=crop&w=1000&q=80", "Sylwia Bartyzel"),
    GalleryItem("Marruecos", "Marrakech", "Medina de Marrakech", Region.AFRICA, "3.2 M", "https://images.unsplash.com/photo-1597212618440-806262de4f6b?auto=format&fit=crop&w=1000&q=80", "Calin Stan"),
    GalleryItem("Egipto", "Giza", "Pirámides de Giza", Region.AFRICA, "14 M", "https://images.unsplash.com/photo-1539650116574-75c0c6d73f74?auto=format&fit=crop&w=1000&q=80", "Spencer Davis"),
    GalleryItem("Australia", "Sídney", "Ópera de Sídney", Region.OCEANIA, "10.9 M", "https://images.unsplash.com/photo-1523482580672-f109ba8cb9be?auto=format&fit=crop&w=1000&q=80", "Caleb"),
    GalleryItem("Nueva Zelanda", "Milford Sound", "Fiordos de Milford", Region.OCEANIA, "1.0 M", "https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=1000&q=80", "Sebastian Pena Lambarri")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MultinationalGalleryScreen() {
    var searchText by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf<Region?>(null) }

    val filteredItems = remember(searchText, selectedRegion) {
        galleryItems.filter { item ->
            val matchesSearch = searchText.isBlank() ||
                item.country.contains(searchText, ignoreCase = true) ||
                item.city.contains(searchText, ignoreCase = true) ||
                item.location.contains(searchText, ignoreCase = true)
            val matchesRegion = selectedRegion == null || item.region == selectedRegion
            matchesSearch && matchesRegion
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Outlined.TravelExplore,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                placeholder = { Text(stringResource(id = R.string.search_hint)) },
                singleLine = true
            )

            Text(
                text = stringResource(id = R.string.cta_filters),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { selectedRegion = null },
                    label = { Text(stringResource(id = R.string.all_regions)) }
                )
                Region.entries.forEach { region ->
                    AssistChip(
                        onClick = { selectedRegion = region },
                        label = { Text(region.toLabel()) }
                    )
                }
            }

            if (filteredItems.isEmpty()) {
                EmptyState(modifier = Modifier.fillMaxWidth())
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 220.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems) { item ->
                        GalleryCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun Region.toLabel(): String = when (this) {
    Region.EUROPE -> stringResource(id = R.string.region_europe)
    Region.AMERICAS -> stringResource(id = R.string.region_americas)
    Region.ASIA -> stringResource(id = R.string.region_asia)
    Region.AFRICA -> stringResource(id = R.string.region_africa)
    Region.OCEANIA -> stringResource(id = R.string.region_oceania)
}

@Composable
fun GalleryCard(item: GalleryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = "${item.location} - ${item.country}",
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(item.location, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${item.city}, ${item.country}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.visitors, item.yearlyVisitors),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(id = R.string.photo_from, item.author),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(id = R.string.empty_message),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GalleryPreview() {
    MaterialTheme {
        MultinationalGalleryScreen()
    }
}
