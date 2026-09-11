package com.biblionet.ui.view.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biblionet.R
import com.biblionet.data.model.transaction.Prestamo
import com.biblionet.ui.viewmodel.transaction.PrestamoViewModel
import java.text.SimpleDateFormat
import androidx.compose.material.icons.filled.AssignmentReturn
import com.biblionet.ui.viewmodel.book.BookViewModel
import com.biblionet.ui.viewmodel.user.UserViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionReservasScreen(
    viewModel: PrestamoViewModel = viewModel(),
    bookViewModel: BookViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    onBack: () -> Unit
) {
    // Eliminamos 'initial = emptyMap()' para que el compilador infiera correctamente el tipo desde StateFlow
    val names by bookViewModel.libroNombres.collectAsState()
    val userNames by userViewModel.userNames.collectAsState()
    val prestamos = viewModel.prestamosPendientes

    val isLoading = viewModel.isLoading
    val error = viewModel.error

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(prestamos) {
        if (prestamos.isNotEmpty()) {
            val ids = prestamos.map { it.libro_id }
            bookViewModel.cargarNombresMasivos(ids)
            userViewModel.cargarNombresUsuarios(prestamos.map { it.usuario_id })
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.gestion_reservas),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.atras),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF9800)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            if (isLoading && prestamos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1A237E))
                }
            } else if (prestamos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.no_reservas_pendientes), color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(prestamos) { prestamo ->
                        val libro_titulo = names[prestamo.libro_id] ?: stringResource(R.string.loading)
                        
                        // Obtenemos el nombre del usuario desde el userNames del UserViewModel
                        val nombre_usuario = userNames[prestamo.usuario_id] ?: stringResource(R.string.loading)

                        ReservaItem(
                            prestamo = prestamo,
                            tituloLibro = libro_titulo,
                            nombreUsuario = nombre_usuario,
                            onAceptar = { viewModel.aceptarReserva(prestamo) },
                            onRechazar = { viewModel.rechazarReserva(prestamo.prestamoid) },
                            onDevolver = { viewModel.devolverLibro(prestamo) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ReservaItem(
    prestamo: Prestamo,
    tituloLibro: String,
    nombreUsuario: String,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
    onDevolver: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(R.string.id_label, tituloLibro),
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = when(prestamo.estado) {
                        "activo" -> Color(0xFFE3F2FD)
                        "retrasado" -> Color(0xFFFFEBEE)
                        else -> Color(0xFFF5F5F5)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when(prestamo.estado) {
                            "activo" -> stringResource(R.string.status_active).uppercase()
                            "retrasado" -> stringResource(R.string.status_overdue).uppercase()
                            "reservado" -> stringResource(R.string.status_reserved).uppercase()
                            "pendiente" -> stringResource(R.string.status_pending).uppercase()
                            else -> prestamo.estado.uppercase()
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when(prestamo.estado) {
                            "activo" -> Color(0xFF1976D2)
                            "retrasado" -> Color.Red
                            else -> Color.Gray
                        }
                    )
                }
            }

            Text(
                fontSize = 14.sp,
                text = stringResource(R.string.usuario_nombre, nombreUsuario),
                color = Color.Gray
            )
            Text(
                text = stringResource(R.string.isbn_label, prestamo.isbn),
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                text = stringResource(R.string.fecha_devolucion_label, dateFormat.format(Date(prestamo.fecha_devolucion))),
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (prestamo.estado == "pendiente" || prestamo.estado == "reservado") {
                    IconButton(onClick = onRechazar, colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFFFEBEE))) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.rechazar), tint = Color.Red)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onAceptar, colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFE8F5E9))) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.aceptar), tint = Color(0xFF2E7D32))
                    }
                }
                else if (prestamo.estado == "activo" || prestamo.estado == "retrasado") {
                    Button(
                        onClick = onDevolver,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = stringResource(R.string.devolver_libro))
                    }
                }
            }
        }
    }
}
