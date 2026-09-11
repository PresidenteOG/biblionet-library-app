// Archivo: src/main/java/cat/copernic/biblionet/ui/view/library/EditLibraryScreen.kt
package com.biblionet.ui.view.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biblionet.R
import com.biblionet.data.model.library.Biblioteca
import com.biblionet.ui.viewmodel.user.AdminViewModel
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBibliotecaScreen(bibliotecaId: String, onSuccess: () -> Unit, onBack: () -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var latitud by remember { mutableStateOf("") }
    var longitud by remember { mutableStateOf("") }
    var accesibilidad by remember { mutableStateOf(false) }

    var fotoUrl by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    val adminViewModel: AdminViewModel = viewModel()
    val context = LocalContext.current

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            adminViewModel.uploadLibraryImage(context, uri) { url ->
                if (url != null) fotoUrl = url
                isUploading = false
            }
        }
    }

    val diasSemanaDB = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo", "Festivos")
    var horariosInput by remember { mutableStateOf(diasSemanaDB.associateWith { "" }.toMutableMap()) }

    LaunchedEffect(bibliotecaId) {
        FirebaseFirestore.getInstance().collection("bibliotecas").document(bibliotecaId).get().addOnSuccessListener { doc ->
            val b = doc.toObject(Biblioteca::class.java)
            if (b != null) {
                nombre = b.nombre; direccion = b.direccion; latitud = b.latitud.toString(); longitud = b.longitud.toString(); accesibilidad = b.accesibilidad
                telefono = b.telefono; email = b.email // CARGA DE DATOS
                fotoUrl = doc.getString("foto_url") ?: doc.getString("fotoUrl") ?: ""
                if (b.horario.isNotEmpty()) {
                    horariosInput = b.horario.mapValues { it.value.joinToString(", ") }.toMutableMap()
                }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.editar_biblioteca)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } }) }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable { imageLauncher.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isUploading) {
                            CircularProgressIndicator(color = Color(0xFF1A237E))
                        } else if (fotoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = fotoUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color(0xFF1A237E), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Añadir / Cambiar Foto", color = Color(0xFF1A237E), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text(stringResource(R.string.nombre_biblioteca)) }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = direccion, onValueChange = { direccion = it }, label = { Text(stringResource(R.string.direccion_biblioteca)) }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                // --- NUEVO: TELÉFONO Y EMAIL ---
                Row {
                    OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Teléfono") }, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                // -------------------------------

                Row {
                    OutlinedTextField(value = latitud, onValueChange = { latitud = it }, label = { Text(stringResource(R.string.latitud)) }, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(value = longitud, onValueChange = { longitud = it }, label = { Text(stringResource(R.string.longitud)) }, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = accesibilidad, onCheckedChange = { accesibilidad = it })
                    Text(stringResource(R.string.accesible_silla_ruedas))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.horarios_apertura), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            }

            items(diasSemanaDB) { diaDb ->
                val diaLocalizado = when(diaDb) {
                    "Lunes" -> stringResource(R.string.dia_lunes)
                    "Martes" -> stringResource(R.string.dia_martes)
                    "Miércoles" -> stringResource(R.string.dia_miercoles)
                    "Jueves" -> stringResource(R.string.dia_jueves)
                    "Viernes" -> stringResource(R.string.dia_viernes)
                    "Sábado" -> stringResource(R.string.dia_sabado)
                    "Domingo" -> stringResource(R.string.dia_domingo)
                    else -> stringResource(R.string.dia_festivos)
                }

                EditDayScheduleRow(
                    diaNombreDisplay = diaLocalizado,
                    horarioActual = horariosInput[diaDb] ?: "",
                    onHorarioChange = { nuevoHorario -> horariosInput = horariosInput.toMutableMap().apply { put(diaDb, nuevoHorario) } }
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val horarioParaFirebase = horariosInput.mapValues { entry ->
                            if (entry.value.isNotBlank()) listOf(entry.value) else emptyList()
                        }
                        val updates = mapOf(
                            "nombre" to nombre, "direccion" to direccion,
                            "telefono" to telefono, "email" to email, // ACTUALIZACIÓN EN FIREBASE
                            "latitud" to latitud.toDoubleOrNull(), "longitud" to longitud.toDoubleOrNull(),
                            "accesibilidad" to accesibilidad, "foto_url" to fotoUrl, "horario" to horarioParaFirebase
                        )
                        FirebaseFirestore.getInstance().collection("bibliotecas").document(bibliotecaId).update(updates).addOnSuccessListener { onSuccess() }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))
                ) { Text(stringResource(R.string.guardar_cambios), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun EditDayScheduleRow(diaNombreDisplay: String, horarioActual: String, onHorarioChange: (String) -> Unit) {
    val isAbierto = horarioActual.isNotBlank() && !horarioActual.equals("cerrado", ignoreCase = true)
    var hApertura by remember(horarioActual) { mutableStateOf(if (isAbierto) horarioActual.split("-").getOrElse(0){"09:00"} else "09:00") }
    var hCierre by remember(horarioActual) { mutableStateOf(if (isAbierto) horarioActual.split("-").getOrElse(1){"20:00"} else "20:00") }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Column(modifier = Modifier.weight(0.35f)) {
            Text(diaNombreDisplay, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = isAbierto, onCheckedChange = { open -> if (open) onHorarioChange("$hApertura-$hCierre") else onHorarioChange("") }, modifier = Modifier.scale(0.8f))
            }
        }

        if (isAbierto) {
            Column(modifier = Modifier.weight(0.65f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.abre_hora), fontSize = 13.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    EditTimePickerBtn(hApertura) { newTime -> hApertura = newTime; onHorarioChange("$newTime-$hCierre") }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cierra_hora), fontSize = 13.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    EditTimePickerBtn(hCierre) { newTime -> hCierre = newTime; onHorarioChange("$hApertura-$newTime") }
                }
            }
        } else {
            Text(stringResource(R.string.cerrado), color = Color(0xFFD32F2F), modifier = Modifier.weight(0.65f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EditTimePickerBtn(time: String, onTimeSelected: (String) -> Unit) {
    val context = LocalContext.current
    Text(
        text = time, color = Color(0xFF1A237E), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
        modifier = Modifier.clickable {
            val parts = time.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            android.app.TimePickerDialog(context, { _, hour, minute ->
                onTimeSelected(String.format("%02d:%02d", hour, minute))
            }, h, m, true).show()
        }.background(Color(0xFFE8EAF6), RoundedCornerShape(8.dp)).padding(horizontal = 24.dp, vertical = 8.dp)
    )
}