// Archivo: cat/copernic/biblionet/ui/view/library/SolicitarReservaScreen.kt
package com.biblionet.ui.view.library

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
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
import com.biblionet.data.model.inventory.Inventario // IMPORT AÑADIDO
import com.biblionet.ui.viewmodel.book.BookViewModel
import com.biblionet.utils.GlobalState.usuario
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitarReservaScreen(
    libroId: String,
    onBackClick: () -> Unit,
    onReservaConfirmada: () -> Unit,
    viewModel: BookViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var bibliotecaSeleccionada by remember { mutableStateOf<String?>(null) }
    var fechaSeleccionada by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isLoadingLocal by remember { mutableStateOf(false) }

    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    LaunchedEffect(libroId) {
        viewModel.loadLibro(libroId)
        viewModel.cargarBibliotecasConStock(libroId)
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { fechaSeleccionada = datePickerState.selectedDateMillis ?: System.currentTimeMillis(); showDatePicker = false }) { Text(stringResource(R.string.accept)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancelar)) } }
        ) { DatePicker(state = datePickerState) }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") }
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.request_reserve_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF1E2A78)) }
        } else {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = uiState.imagen_url, contentDescription = null, modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = stringResource(R.string.sedes_count, uiState.librariesMap.size), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text(text = "ISBN: ${uiState.isbn}", fontSize = 13.sp, color = Color.DarkGray)
                        }
                    }
                }

                Text(text = stringResource(R.string.where_collect), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))

                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    if (uiState.isLoadingInventario) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                    } else if (uiState.inventories.isEmpty()) {
                        Text(text = stringResource(R.string.no_stock_available), modifier = Modifier.padding(16.dp), color = Color.Red)
                    } else {
                        uiState.inventories.forEach { inventario ->
                            val biblio = uiState.librariesMap[inventario.biblioteca_id]
                            val nombreBiblio = biblio?.nombre ?: stringResource(R.string.unknown_library)
                            Row(modifier = Modifier.fillMaxWidth().clickable { bibliotecaSeleccionada = inventario.biblioteca_id }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = (bibliotecaSeleccionada == inventario.biblioteca_id), onClick = { bibliotecaSeleccionada = inventario.biblioteca_id })
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(nombreBiblio, fontWeight = FontWeight.Medium)
                                    Text(text = stringResource(R.string.available_copies, inventario.stock_disponible), fontSize = 12.sp, color = if (inventario.stock_disponible > 0) Color.Gray else Color.Red)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.estimated_pickup_date), fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Event, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(formatter.format(Date(fechaSeleccionada)))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val sedeActual = uiState.inventories.find { it.biblioteca_id == bibliotecaSeleccionada }
                        val tieneStock = (sedeActual?.stock_disponible ?: 0) > 0
                        val ahora = com.google.firebase.Timestamp.now()

                        if (usuario?.bloqueado_hasta == null || usuario?.bloqueado_hasta!! < ahora) {
                            Button(
                                onClick = {
                                    bibliotecaSeleccionada?.let { sedeId ->
                                        isLoadingLocal = true
                                        viewModel.cargarNombreLibro(libroId)
                                        viewModel.solicitarPrestamo(
                                            libroId = libroId, isbn = uiState.isbn, bibliotecaId = sedeId, fechaDevolucion = fechaSeleccionada,
                                            onSuccess = { isLoadingLocal = false; onReservaConfirmada() },
                                            onError = { error -> isLoadingLocal = false; Log.e("RESERVA", "Error: $error") }
                                        )
                                    }
                                },
                                enabled = bibliotecaSeleccionada != null && !isLoadingLocal && tieneStock,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(26.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2A78))
                            ) {
                                if (isLoadingLocal) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Text(stringResource(R.string.confirm_reserve), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(text = String.format(stringResource(R.string.cuenta_bloqueada), formatter.format(usuario?.bloqueado_hasta?.toDate())), fontSize = 15.sp, color = Color.Red)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}