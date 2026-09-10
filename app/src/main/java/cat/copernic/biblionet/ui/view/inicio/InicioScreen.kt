// Archivo: InicioScreen.kt
package cat.copernic.biblionet.ui.view.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import cat.copernic.biblionet.R
import cat.copernic.biblionet.data.model.auth.Usuario
import cat.copernic.biblionet.ui.components.LibroCard
import cat.copernic.biblionet.ui.viewmodel.inicio.InicioViewModel
import cat.copernic.biblionet.ui.viewmodel.inicio.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    usuario: Usuario,
    navController: NavHostController,
    viewModel: InicioViewModel = viewModel(),
    onProfileClick: () -> Unit
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFDFDFD))
        ) {
            HeaderSection(usuario = usuario, onProfileClick = onProfileClick)

            SearchBarSection(
                viewModel = viewModel,
                onFilterClick = { showFilterSheet = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.libros.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.no_libros_encontrados), color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(viewModel.libros) { libro ->
                        LibroCard(
                            libro = libro,
                            rating = libro.calificacion.toDoubleOrNull() ?: 0.0,
                            isFavorite = false,
                            onClick = {
                                navController.navigate("LibroDetail/${libro.libro_id}")
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            viewModel = viewModel,
            sheetState = sheetState,
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
fun HeaderSection(usuario: Usuario, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.nav_inicio), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
            Text(text = String.format(stringResource(R.string.hola_usuario), usuario.nombre), fontSize = 14.sp, color = Color.Gray)
        }
        IconButton(onClick = onProfileClick, modifier = Modifier.size(45.dp)) {
            AsyncImage(
                model = usuario.foto_url,
                contentDescription = "Ir al perfil",
                modifier = Modifier.fillMaxSize().clip(CircleShape).border(0.5.dp, Color(0xFF1A237E), CircleShape).background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun SearchBarSection(viewModel: InicioViewModel, onFilterClick: () -> Unit) {
    val isFilterActive = viewModel.selectedCategoriaId != "Todos" || 
                         viewModel.sortOrder != SortOrder.NONE || 
                         viewModel.selectedRating > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier.weight(1f),
            placeholder = { Text(text = stringResource(R.string.buscar_titulo_autor)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1A237E),
                unfocusedContainerColor = Color(0xFFF1F4F8),
                unfocusedBorderColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Surface(
            onClick = onFilterClick,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (isFilterActive) Color(0xFF1A237E) else Color(0xFFF1F4F8),
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = stringResource(R.string.filtros),
                    tint = if (isFilterActive) Color.White else Color(0xFF1A237E),
                    modifier = Modifier.size(24.dp)
                )

                if (isFilterActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(8.dp)
                            .background(Color(0xFFFFB74D), CircleShape)
                            .border(1.dp, Color.White, CircleShape)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    viewModel: InicioViewModel,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    var categorySearchQuery by remember { mutableStateOf("") }

    val filteredCategorias = remember(categorySearchQuery, viewModel.categorias.toList()) {
        if (categorySearchQuery.isBlank()) {
            viewModel.categorias.toList()
        } else {
            viewModel.categorias.filter { it.nombre.contains(categorySearchQuery, ignoreCase = true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filtros),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A237E)
                )
                TextButton(onClick = {
                    viewModel.onSearchQueryChange("")
                    viewModel.onCategoriaSelected("Todos")
                    viewModel.onSortOrderSelected(SortOrder.NONE)
                    viewModel.onRatingSelected(0)
                }) {
                    Text(stringResource(R.string.reiniciar), color = Color(0xFFD32F2F))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección: Ordenar por fecha
            Text(
                text = stringResource(R.string.ordenar_por_fecha),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Opción: Recientes
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = viewModel.sortOrder == SortOrder.NEWEST,
                    onClick = { viewModel.onSortOrderSelected(SortOrder.NEWEST) },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.mas_recientes), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1A237E),
                        selectedLabelColor = Color.White
                    )
                )

                // Opción: Antiguos
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = viewModel.sortOrder == SortOrder.OLDEST,
                    onClick = { viewModel.onSortOrderSelected(SortOrder.OLDEST) },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.mas_antiguos), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1A237E),
                        selectedLabelColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección: Ordenar por valoración
            Text(
                text = stringResource(R.string.ordenar_por_valoracion),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Opción: Mejor valoración
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = viewModel.sortOrder == SortOrder.DESCENDING,
                    onClick = { viewModel.onSortOrderSelected(SortOrder.DESCENDING) },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp), tint = if(viewModel.sortOrder == SortOrder.DESCENDING) Color.White else Color(0xFFFFB74D))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.mejor_valoracion), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1A237E),
                        selectedLabelColor = Color.White
                    )
                )
                
                // Opción: Peor valoración
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = viewModel.sortOrder == SortOrder.ASCENDING,
                    onClick = { viewModel.onSortOrderSelected(SortOrder.ASCENDING) },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.StarOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.peor_valoracion), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1A237E),
                        selectedLabelColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.filtrar_por_estrellas),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (rating in 1..5) {
                    FilterChip(
                        selected = viewModel.selectedRating == rating,
                        onClick = { 
                            if (viewModel.selectedRating == rating) viewModel.onRatingSelected(0)
                            else viewModel.onRatingSelected(rating)
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = rating.toString())
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (viewModel.selectedRating == rating) Color.White else Color(0xFFFFB74D)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1A237E),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.seleccionar_categoria),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = categorySearchQuery,
                onValueChange = { categorySearchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.buscar_categoria)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1A237E),
                    unfocusedContainerColor = Color(0xFFF1F4F8),
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = viewModel.selectedCategoriaId == "Todos",
                        onClick = { viewModel.onCategoriaSelected("Todos") },
                        label = { Text(stringResource(R.string.todas)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1A237E),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                items(filteredCategorias) { categoria ->
                    FilterChip(
                        selected = viewModel.selectedCategoriaId == categoria.categoriaId,
                        onClick = { viewModel.onCategoriaSelected(categoria.categoriaId) },
                        label = { Text(categoria.nombre) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1A237E),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))
            ) {
                Text(
                    stringResource(R.string.aplicar_filtros),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
