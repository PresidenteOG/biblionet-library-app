// Archivo: cat/copernic/biblionet/ui/viewmodel/admin/AdminQuizScreens.kt
package cat.copernic.biblionet.ui.viewmodel.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cat.copernic.biblionet.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQuizListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToForm: (String?) -> Unit,
    viewModel: AdminQuizViewModel = viewModel()
) {
    val context = LocalContext.current
    val quizRestartedMsg = stringResource(R.string.quiz_restarted_msg)

    LaunchedEffect(Unit) {
        viewModel.cargarPreguntas()
    }

    var preguntaABorrar by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_quiz_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.reset_quiz_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        // CORRECCIÓN: Llamamos al nombre correcto
                        viewModel.reiniciarQuizGlobalmente {
                            showResetDialog = false
                            Toast.makeText(context, quizRestartedMsg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))
                ) {
                    Text(stringResource(R.string.reset_now), color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancelar), color = Color(0xFF1A237E))
                }
            }
        )
    }

    if (preguntaABorrar != null) {
        AlertDialog(
            onDismissRequest = { preguntaABorrar = null },
            title = { Text(stringResource(R.string.delete_question_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_question_desc)) },
            confirmButton = {
                Button(onClick = { viewModel.eliminarPregunta(preguntaABorrar!!); preguntaABorrar = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))) {
                    Text(stringResource(R.string.delete), color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { preguntaABorrar = null }) { Text(stringResource(R.string.cancelar), color = Color(0xFF1A237E)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_daily_quiz), fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.atras), tint = Color.White) }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset_quiz_icon_desc), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A237E))
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToForm(null) }, containerColor = Color(0xFFFFB300), contentColor = Color(0xFF1A237E)) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_question))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA))) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF1A237E))
            } else if (viewModel.preguntas.isEmpty()) {
                Text(stringResource(R.string.no_questions_admin), color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val letras = listOf("A", "B", "C", "D")
                    items(viewModel.preguntas) { pregunta ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pregunta.texto, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(String.format(stringResource(R.string.options_correct_format), pregunta.opciones.size, letras.getOrNull(pregunta.indiceCorrecto) ?: "?"), fontSize = 12.sp, color = Color.Gray)
                                }
                                Row {
                                    IconButton(onClick = { onNavigateToForm(pregunta.id) }) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit), tint = Color(0xFF3B82F6)) }
                                    IconButton(onClick = { preguntaABorrar = pregunta.id }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color(0xFFE11D48)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQuizFormScreen(
    preguntaId: String?,
    onNavigateBack: () -> Unit,
    viewModel: AdminQuizViewModel = viewModel()
) {
    var texto by remember { mutableStateOf("") }
    var opcion1 by remember { mutableStateOf("") }
    var opcion2 by remember { mutableStateOf("") }
    var opcion3 by remember { mutableStateOf("") }
    var opcion4 by remember { mutableStateOf("") }
    var indiceCorrecto by remember { mutableStateOf(0) }

    val scrollState = rememberScrollState()
    val letras = listOf("A", "B", "C", "D")

    LaunchedEffect(preguntaId, viewModel.preguntas) {
        if (preguntaId != null) {
            val p = viewModel.preguntas.find { it.id == preguntaId }
            if (p != null) {
                texto = p.texto
                opcion1 = p.opciones.getOrNull(0) ?: ""
                opcion2 = p.opciones.getOrNull(1) ?: ""
                opcion3 = p.opciones.getOrNull(2) ?: ""
                opcion4 = p.opciones.getOrNull(3) ?: ""
                indiceCorrecto = p.indiceCorrecto
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (preguntaId == null) stringResource(R.string.new_question) else stringResource(R.string.edit_question), fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.atras), tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A237E))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA)).verticalScroll(scrollState).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = texto, onValueChange = { texto = it }, label = { Text(stringResource(R.string.question_label)) }, modifier = Modifier.fillMaxWidth())

            Text(stringResource(R.string.options_fill_all), fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

            OutlinedTextField(value = opcion1, onValueChange = { opcion1 = it }, label = { Text(stringResource(R.string.option_a)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = opcion2, onValueChange = { opcion2 = it }, label = { Text(stringResource(R.string.option_b)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = opcion3, onValueChange = { opcion3 = it }, label = { Text(stringResource(R.string.option_c)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = opcion4, onValueChange = { opcion4 = it }, label = { Text(stringResource(R.string.option_d)) }, modifier = Modifier.fillMaxWidth())

            Text(stringResource(R.string.select_correct_option), fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                letras.forEachIndexed { index, letra ->
                    FilterChip(
                        selected = indiceCorrecto == index,
                        onClick = { indiceCorrecto = index },
                        label = { Text(String.format(stringResource(R.string.op_format), letra)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1A237E), selectedLabelColor = Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = onNavigateBack, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.discard), color = Color(0xFFE11D48), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val opcionesValidas = listOf(opcion1, opcion2, opcion3, opcion4).filter { it.isNotBlank() }
                        if (texto.isNotBlank() && opcionesValidas.size == 4) {
                            viewModel.guardarPregunta(
                                id = preguntaId ?: "",
                                texto = texto,
                                opciones = opcionesValidas,
                                indiceCorrecto = indiceCorrecto,
                                onSuccess = onNavigateBack
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !viewModel.isLoading
                ) {
                    Text(if (viewModel.isLoading) stringResource(R.string.saving) else stringResource(R.string.save), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}