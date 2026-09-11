package com.biblionet.ui.view.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biblionet.R
import com.biblionet.data.model.auth.Role
import com.biblionet.data.model.book.Libro
import com.biblionet.ui.viewmodel.book.LibrosViewModel
import com.biblionet.utils.GlobalState.usuario
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibliotecarioPanelScreen(
    librosViewModel: LibrosViewModel = viewModel(),
    onNavigateToCrear: () -> Unit,
    onNavigateToDetalle: (String) -> Unit,
    onNavigateToEditar: (String) -> Unit,
    onNavigateToInventario: () -> Unit,
    onNavigateToReservas: () -> Unit,
    onBack: () -> Unit,
    onNavigateToCategorias: () -> Unit
) {
    val libros = librosViewModel.libros
    val isLoading = librosViewModel.isLoading
    val errorMessage = librosViewModel.errorMessage
    val errorResId = librosViewModel.errorResId

    var searchText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    val filteredLibros = if (searchText.isEmpty()) {
        libros
    } else {
        libros.filter {
            it.titulo.contains(searchText, ignoreCase = true) ||
                    it.autor.contains(searchText, ignoreCase = true) ||
                    it.isbn.contains(searchText, ignoreCase = true)
        }
    }

    // Dialogo de confirmación de borrado
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.eliminar_libro)) },
            text = { Text(stringResource(R.string.confirmar_eliminar_libro)) },
            confirmButton = {
                TextButton(onClick = {
                    librosViewModel.eliminarLibro(showDeleteDialog!!)
                    showDeleteDialog = null
                }) {
                    // Usamos el color rojo para acciones destructivas
                    Text(stringResource(R.string.delete), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }

    // Diálogo de error multi-idioma
    if (errorResId != null) {
        AlertDialog(
            onDismissRequest = { librosViewModel.clearError() },
            title = {
                Text(
                    text = stringResource(R.string.atencion), // Asegúrate de tener este string
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                // Aquí es donde ocurre la magia del multi-idioma
                Text(stringResource(id = errorResId))
            },
            confirmButton = {
                Button(onClick = { librosViewModel.clearError() }) {
                    Text(stringResource(R.string.entendido))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.librarian_panel),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A237E)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.atras))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1A237E))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(R.string.quick_actions),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                PanelActionButton(
                                    text = stringResource(R.string.inventory),
                                    icon = Icons.Default.Inventory2,
                                    color = Color(0xFF1A237E),
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToInventario
                                )
                                PanelActionButton(
                                    text = stringResource(R.string.categories),
                                    icon = Icons.Default.Label,
                                    color = Color(0xFF3949AB),
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToCategorias
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                PanelActionButton(
                                    text = stringResource(R.string.tab_reservations),
                                    icon = Icons.Default.Bookmark,
                                    color = Color(0xFFF57C00),
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToReservas
                                )
                                PanelActionButton(
                                    text = stringResource(R.string.create_new_book),
                                    icon = Icons.Default.AddCircle,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToCrear
                                )
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            label = { Text(stringResource(R.string.search_title_author_isbn)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    IconButton(onClick = { searchText = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                                    }
                                }
                            }
                        )
                    }

                    if (filteredLibros.isEmpty() && searchText.isNotEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.no_libros_encontrados), color = Color.Gray)
                            }
                        }
                    } else {
                        items(filteredLibros) { libro ->
                            LibroAdminCard(
                                libro = libro,
                                onEdit = { onNavigateToEditar(libro.libro_id) },
                                onDelete = { showDeleteDialog = libro.libro_id },
                                onClick = { onNavigateToDetalle(libro.libro_id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibroAdminCard(
    libro: Libro,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = libro.imagen_url.ifEmpty { null },
                contentDescription = null,
                modifier = Modifier
                    .width(60.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = libro.titulo, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E), maxLines = 1)
                Text(text = libro.autor, fontSize = 14.sp, color = Color.Gray, maxLines = 1)
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit), tint = Color(0xFF1A237E))
                }
                if (usuario?.rol == Role.ADMIN ) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color(0xFFD32F2F))
                    }
                }

            }
        }
    }
}

@Composable
fun PanelActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
