// Archivo: cat/copernic/biblionet/ui/view/inventory/InventarioFormScreen.kt
package com.biblionet.ui.view.inventory

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biblionet.R
import com.biblionet.data.model.book.Libro
import com.biblionet.data.model.inventory.Inventario // IMPORT AÑADIDO
import com.biblionet.ui.viewmodel.book.BookViewModel
import com.biblionet.ui.viewmodel.book.LibrosViewModel
import com.biblionet.utils.GlobalState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioFormScreen(
    bookViewModel: BookViewModel = viewModel(),
    librosViewModel: LibrosViewModel = viewModel(),
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val usuarioActual = GlobalState.usuario
    val libros = librosViewModel.libros
    val nombreSede by bookViewModel.nombreBibliotecaUsuario.collectAsState()

    val uiState by bookViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var libroSeleccionado by remember { mutableStateOf<Libro?>(null) }
    var showLibrosDialog by remember { mutableStateOf(false) }
    var stockTotal by remember { mutableStateOf("") }
    var pasilloEstanteria by remember { mutableStateOf("") }

    LaunchedEffect(usuarioActual) {
        usuarioActual?.biblioteca_id?.let { id ->
            if (id.isNotEmpty()) bookViewModel.cargarNombreBiblioteca(id)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { mensaje ->
            snackbarHostState.showSnackbar(message = mensaje, duration = SnackbarDuration.Short)
            bookViewModel.clearError()
        }
    }

    val isFormValid = libroSeleccionado != null && stockTotal.toIntOrNull() != null && usuarioActual?.biblioteca_id?.isNotEmpty() == true

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_inventory_title), fontWeight = FontWeight.Bold, color = Color(0xFF1A237E)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.atras)) } },
                actions = {
                    IconButton(
                        onClick = {
                            if (isFormValid) {
                                val copias = stockTotal.toIntOrNull() ?: 0
                                val nuevoItem = Inventario(
                                    libro_id = libroSeleccionado!!.libro_id,
                                    biblioteca_id = usuarioActual!!.biblioteca_id,
                                    numero_copias = copias,
                                    stock_disponible = copias,
                                    pasilloEstanteria = pasilloEstanteria,
                                    disponible = copias > 0
                                )
                                bookViewModel.crearInventario(nuevoItem) { onSave() }
                            }
                        },
                        enabled = isFormValid && !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save), tint = if (isFormValid) Color(0xFF1A237E) else Color.Gray)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5)).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF1A237E))
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(stringResource(R.string.work_location), fontSize = 12.sp, color = Color.Gray)
                        Text(text = nombreSede, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E), fontSize = 16.sp)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.select_book_asterisk), fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                    Button(onClick = { showLibrosDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))) {
                        Text(stringResource(R.string.search_catalog))
                    }
                    libroSeleccionado?.let { libro ->
                        ListItem(headlineContent = { Text(libro.titulo, fontWeight = FontWeight.Bold) }, supportingContent = { Text(libro.autor) }, colors = ListItemDefaults.colors(containerColor = Color(0xFFF0F4FF)))
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.stock_details), fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                    OutlinedTextField(
                        value = stockTotal, onValueChange = { if (it.all { c -> c.isDigit() }) stockTotal = it },
                        label = { Text(stringResource(R.string.number_of_copies_asterisk)) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = pasilloEstanteria, onValueChange = { pasilloEstanteria = it },
                        label = { Text(stringResource(R.string.physical_location)) }, placeholder = { Text(stringResource(R.string.location_placeholder)) }, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showLibrosDialog) {
        AlertDialog(
            onDismissRequest = { showLibrosDialog = false },
            title = { Text(stringResource(R.string.select_book_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.height(400.dp)) {
                    LazyColumn {
                        items(libros) { libro ->
                            ListItem(
                                modifier = Modifier.clickable { libroSeleccionado = libro; showLibrosDialog = false },
                                headlineContent = { Text(libro.titulo) }, supportingContent = { Text(libro.autor) }
                            )
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLibrosDialog = false }) { Text(stringResource(R.string.cerrar)) } }
        )
    }
}