// Archivo: cat/copernic/biblionet/ui/view/inventory/InventarioDetailScreen.kt
package com.biblionet.ui.view.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biblionet.R
import com.biblionet.data.model.inventory.Inventario // IMPORT AÑADIDO
import com.biblionet.ui.viewmodel.book.BookViewModel
import com.biblionet.ui.viewmodel.book.LibrosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioDetailScreen(
    inventarioId: String,
    bookViewModel: BookViewModel = viewModel(),
    librosViewModel: LibrosViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by bookViewModel.uiState.collectAsState()
    val libroNombres by bookViewModel.libroNombres.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val item = uiState.inventories.find { it.id == inventarioId }
    val libroCompleto = librosViewModel.libros.find { it.libro_id == item?.libro_id }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var nuevoStock by remember { mutableStateOf("") }
    var nuevoPasillo by remember { mutableStateOf("") }

    LaunchedEffect(inventarioId) {
        bookViewModel.cargarItemInventarioEspecifico(inventarioId)
    }

    LaunchedEffect(item) {
        item?.let {
            nuevoStock = it.numero_copias.toString()
            nuevoPasillo = it.pasilloEstanteria
            if (!libroNombres.containsKey(it.libro_id)) {
                bookViewModel.cargarNombreLibro(it.libro_id)
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            bookViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.inventory_management), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.atras)) }
                },
                actions = {
                    if (item != null) {
                        IconButton(onClick = { showEditDialog = true }) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit), tint = Color(0xFF1A237E)) }
                        IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color.Red) }
                    }
                }
            )
        }
    ) { padding ->
        if (item == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF1A237E)) }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA)).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text(text = libroNombres[item.libro_id] ?: stringResource(R.string.loading_title), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A237E))
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                            Spacer(Modifier.width(6.dp))
                            val notAvailable = stringResource(R.string.not_available_short)
                            Text(text = "ISBN: ${libroCompleto?.isbn ?: notAvailable}", fontSize = 14.sp, color = Color.Gray)
                        }
                        HorizontalDivider(Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)
                        Surface(color = if (item.disponible) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = if (item.disponible) stringResource(R.string.in_catalog) else stringResource(R.string.out_of_stock_caps),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = if (item.disponible) Color(0xFF2E7D32) else Color.Red
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoCard(modifier = Modifier.weight(1f), label = stringResource(R.string.total_copies), value = item.numero_copias.toString(), icon = Icons.Default.Inventory, containerColor = Color.White)
                    InfoCard(modifier = Modifier.weight(1f), label = stringResource(R.string.available_copies_label), value = item.stock_disponible.toString(), icon = Icons.Default.CheckCircle, containerColor = Color.White)
                }

                val locationUndefined = stringResource(R.string.location_undefined)
                InfoCard(modifier = Modifier.fillMaxWidth(), label = stringResource(R.string.aisle_shelf), value = item.pasilloEstanteria.ifBlank { locationUndefined }, icon = Icons.Default.Place, containerColor = Color(0xFFE8EAF6))
            }
        }
    }

    if (showDeleteDialog && item != null) {
        val canDelete = item.stock_disponible == item.numero_copias
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red) },
            title = { Text(if (canDelete) stringResource(R.string.confirm_deletion) else stringResource(R.string.deletion_blocked)) },
            text = { Text(if (canDelete) stringResource(R.string.delete_item_confirm_desc) else stringResource(R.string.delete_item_blocked_desc)) },
            confirmButton = {
                if (canDelete) {
                    Button(onClick = { bookViewModel.eliminarItemInventario(inventarioId) { onBack() }; showDeleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                        Text(stringResource(R.string.delete_record))
                    }
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(if (canDelete) stringResource(R.string.cancelar) else stringResource(R.string.understood)) } }
        )
    }

    if (showEditDialog && item != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.edit_inventory), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nuevoStock, onValueChange = { if (it.all { c -> c.isDigit() }) nuevoStock = it },
                        label = { Text(stringResource(R.string.total_copies)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nuevoPasillo, onValueChange = { nuevoPasillo = it }, label = { Text(stringResource(R.string.aisle_shelf)) }, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val stockInt = nuevoStock.toIntOrNull() ?: 0
                    bookViewModel.actualizarInventario(inventarioId, stockInt, nuevoPasillo)
                    showEditDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))) { Text(stringResource(R.string.save_changes_btn)) }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text(stringResource(R.string.cancelar)) } }
        )
    }
}

@Composable
fun InfoCard(modifier: Modifier, label: String, value: String, icon: ImageVector, containerColor: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = containerColor), elevation = CardDefaults.cardElevation(1.dp), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF1A237E), modifier = Modifier.size(24.dp))
            Column(Modifier.padding(start = 12.dp)) {
                Text(label, fontSize = 11.sp, color = Color.Gray)
                Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A237E))
            }
        }
    }
}