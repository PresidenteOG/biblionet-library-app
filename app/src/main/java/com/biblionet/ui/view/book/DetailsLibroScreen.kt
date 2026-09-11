package com.biblionet.ui.view.book

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biblionet.R
import com.biblionet.data.model.auth.Role
import com.biblionet.data.model.book.Libro
import com.biblionet.data.model.book.Comentario
import com.biblionet.ui.viewmodel.book.BookViewModel
import com.biblionet.ui.viewmodel.user.UserViewModel
import com.biblionet.utils.GlobalState
import com.biblionet.utils.GlobalState.usuario
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage

/**
 * Pantalla de detalles del libro.
 * Muestra la información completa de un libro, permite gestionarlo como favorito,
 * ver su disponibilidad y gestionar comentarios.
 *
 * @param libroId ID del libro a cargar.
 * @param onBackClick Acción para volver atrás.
 * @param onReservarClick Acción para ir a la pantalla de reserva.
 * @param viewModel ViewModel de libros.
 */
@Composable
fun DetailsLibroScreen(
    libroId: String,
    onBackClick: () -> Unit,
    onReservarClick: () -> Unit,
    viewModel: BookViewModel = viewModel()
) {
    LaunchedEffect(libroId) {
        viewModel.loadLibro(libroId)
    }

    val uiState by viewModel.uiState.collectAsState()
    var nuevoComentarioTexto by remember { mutableStateOf("") }
    var nuevasEstrellas by remember { mutableStateOf(5) }
    var comentarioAEditar by remember { mutableStateOf<Comentario?>(null) }


    LaunchedEffect(libroId) {
        viewModel.cargarBibliotecasConStock(libroId)
    }
    val isAvailable = uiState.inventories.any {
        it.stock_disponible > 0 || it.numero_copias > 0
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF1E2A78))
        }
        return
    }

    val libroData = Libro(
        libro_id = uiState.libro_id ?: "",
        titulo = uiState.titulo,
        autor = uiState.autor,
        isbn = uiState.isbn,
        idioma = uiState.idioma,
        descripcion_corta = uiState.descripcion_corta,
        descripcion_larga = uiState.descripcion_larga,
        imagen_url = uiState.imagen_url,
        categoria_id = uiState.categoria_id,
        calificacion = uiState.calificacion
    )

    // CONTENEDOR PRINCIPAL
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {

        // 1. PARTE FIJA: TopBar y Header
        TopBar(
            isFavorite = uiState.isFavorite,
            onBackClick = onBackClick,
            onFavoriteClick = { viewModel.toggleFavorite(libroId) }
        )


        // 2. PARTE CON SCROLL: El resto del contenido
        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                LibroHeader(libroData)
                LibroInfoCard(libroData, viewModel)
                LibroDescripcion(libroData)
                DisponibilidadSection(isAvailable)

                AddComentarioSection(
                    texto = nuevoComentarioTexto,
                    onTextoChange = { nuevoComentarioTexto = it },
                    estrellas = nuevasEstrellas,
                    onEstrellasChange = { nuevasEstrellas = it },
                    isEditing = (comentarioAEditar != null),
                    onCancelarEdit = {
                        comentarioAEditar = null
                        nuevoComentarioTexto = ""
                        nuevasEstrellas = 5
                    },
                    onEnviarClick = {
                        if (comentarioAEditar == null) {
                            viewModel.addComentario(libroId, nuevoComentarioTexto, nuevasEstrellas)
                        } else {
                            viewModel.updateComentario(
                                libroId = libroId,
                                comentario = comentarioAEditar!!,
                                nuevoTexto = nuevoComentarioTexto,
                                nuevasEstrellas = nuevasEstrellas
                            )
                        }
                        comentarioAEditar = null
                        nuevoComentarioTexto = ""
                        nuevasEstrellas = 5
                    }
                )

                ReseñasSection(
                    comentarios = uiState.comentarios,
                    viewModel = viewModel,
                    onEditClick = { comentarioSeleccionado ->
                        comentarioAEditar = comentarioSeleccionado
                        nuevoComentarioTexto = comentarioSeleccionado.comentario_texto
                        nuevasEstrellas = comentarioSeleccionado.estrellas
                    }
                )

                Spacer(modifier = Modifier.height(100.dp))
            }

            // 3. BOTÓN FLOTANTE
            BottomReservarButton(
                onClick = onReservarClick,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Barra superior con botón de retroceso, título y botón de favoritos.
 */
@Composable
private fun TopBar(isFavorite: Boolean, onBackClick: () -> Unit, onFavoriteClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, stringResource(R.string.atras)) }
        Text(stringResource(R.string.details_title), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = stringResource(R.string.favorite),
                tint = if (isFavorite) Color.Red else Color.Black
            )
        }
    }
}

/**
 * Encabezado con la imagen de portada, título y autor del libro.
 */
@Composable
private fun LibroHeader(libro: Libro) {

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = libro.imagen_url, contentDescription = null,
            modifier = Modifier.height(220.dp).clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(libro.titulo, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(libro.autor, fontSize = 16.sp, color = Color.Gray)
    }
}

/**
 * Tarjeta de información que muestra la calificación, categoría e idioma.
 * Al hacer clic en la categoría, se muestra un popup con su información detallada.
 */
@Composable
private fun LibroInfoCard(libro: Libro, viewModel: BookViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showCategoriaDialog by remember { mutableStateOf(false) }
    val categoriaObj = uiState.categorias.find { it.categoriaId == libro.categoria_id }

    if (showCategoriaDialog && categoriaObj != null) {
        AlertDialog(
            onDismissRequest = { showCategoriaDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = true), // Mantiene el ancho estándar compacto
            modifier = Modifier.padding(16.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Imagen pequeña estilo "Avatar"
                    if (categoriaObj.imagenUrl.isNotEmpty()) {
                        AsyncImage(
                            model = categoriaObj.imagenUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        text = categoriaObj.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E2A78)
                    )
                }
            },
            text = {
                Text(
                    text = categoriaObj.descripcion.ifEmpty { stringResource(R.string.no_description) },
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    color = Color.Gray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showCategoriaDialog = false },
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text(
                        stringResource(R.string.cancelar), // Cambiado a cancelar o cerrar
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E2A78)
                    )
                }
            },
            shape = RoundedCornerShape(24.dp), // Bordes más suaves y modernos
            containerColor = Color.White
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. SECCIÓN CALIFICACIÓN
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = libro.calificacion,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
                Text(stringResource(R.string.calif), fontSize = 11.sp, color = Color.Gray)
            }

            // 2. SECCIÓN CATEGORÍA
            val categoriaNombre = categoriaObj?.nombre ?: stringResource(R.string.no_category_found)
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showCategoriaDialog = true }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (categoriaNombre == "no_category_found") stringResource(R.string.no_category_found) else categoriaNombre.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E2A78)
                )
                Text(stringResource(R.string.category), fontSize = 11.sp, color = Color.Gray)
            }

            // 3. SECCIÓN IDIOMA
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Translate,
                        contentDescription = null,
                        tint = Color(0xFF1E2A78),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = libro.idioma.let { it.ifBlank { stringResource(R.string.not_available_abbr) } }.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(stringResource(R.string.language), fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

/**
 * Sección que muestra la descripción larga del libro.
 */
@Composable
private fun LibroDescripcion(libro: Libro) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(stringResource(R.string.descripcion), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(libro.descripcion_larga.ifEmpty { stringResource(R.string.no_description) }, fontSize = 14.sp, color = Color.DarkGray)
    }
}

/**
 * Muestra visualmente si el libro está disponible para reserva.
 */
@Composable
private fun DisponibilidadSection(isAvailable: Boolean) {
    val backgroundColor = if (isAvailable) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val textColor = if (isAvailable) Color(0xFF2E7D32) else Color(0xFFC62828)
    val text = if (isAvailable) stringResource(R.string.available_reserve) else stringResource(R.string.not_available)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

/**
 * Sección para añadir o editar un comentario/reseña.
 */
@Composable
private fun AddComentarioSection(
    texto: String,
    onTextoChange: (String) -> Unit,
    estrellas: Int,
    onEstrellasChange: (Int) -> Unit,
    isEditing: Boolean,
    onCancelarEdit: () -> Unit,
    onEnviarClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (isEditing) Color(0xFFE3F2FD) else Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(if (isEditing) stringResource(R.string.edit_review) else stringResource(R.string.write_opinion), fontWeight = FontWeight.Bold)
            Row {
                repeat(5) { i ->
                    IconButton(onClick = { onEstrellasChange(i + 1) }) {
                        Icon(Icons.Default.Star, null, tint = if (i < estrellas) Color(0xFFFFB300) else Color.LightGray)
                    }
                }
            }
            OutlinedTextField(value = texto, onValueChange = onTextoChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.opinion_placeholder)) })
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (isEditing) TextButton(onClick = onCancelarEdit) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = onEnviarClick,
                    enabled = estrellas > 0
                ) {
                    Text(if (isEditing) stringResource(R.string.update) else stringResource(R.string.publish))
                }
            }
        }
    }
}

/**
 * Lista de reseñas de los usuarios.
 */
@Composable
private fun ReseñasSection(
    comentarios: List<Comentario>,
    viewModel: BookViewModel,
    onEditClick: (Comentario) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            stringResource(R.string.user_reviews, comentarios.size),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color(0xFF1A237E)
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (comentarios.isEmpty()) {
            Text(stringResource(R.string.no_reviews), color = Color.Gray)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp) 
                    .verticalScroll(rememberScrollState())
            ) {
                comentarios.forEach { comentario ->
                    ReviewItem(
                        comentario = comentario,
                        bookViewModel = viewModel,
                        onEditClick = onEditClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Elemento individual de reseña que muestra la foto, nombre, estrellas, fecha y texto.
 */
@Composable
private fun ReviewItem(
    comentario: Comentario,
    bookViewModel: BookViewModel,
    onEditClick: (Comentario) -> Unit,
    userViewModel: UserViewModel = viewModel()
) {
    val esAdmin = GlobalState.esAdmin()
    val esMio = usuario?.uid == comentario.usuario_id

    val loadingText = stringResource(R.string.loading)
    var nombreAutor by remember { mutableStateOf(loadingText) }
    var fotoAutor by remember { mutableStateOf("https://res.cloudinary.com/die6u09pk/image/upload/v1771956640/bxazkjut72hrw915jbey.jpg") }

    LaunchedEffect(comentario.usuario_id) {
        nombreAutor = userViewModel.getNombreUsuario(comentario.usuario_id)
        fotoAutor = userViewModel.getFotoUsuario(comentario.usuario_id)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = if (esMio) Color(0xFFF0F4FF) else Color.White),
        elevation = CardDefaults.cardElevation(if (esMio) 4.dp else 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = fotoAutor,
                contentDescription = null,
                modifier = Modifier.size(45.dp).clip(RoundedCornerShape(22.dp)).background(Color.LightGray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    val textoYo = stringResource(R.string.me)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (esMio) textoYo else nombreAutor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (esMio) Color(0xFF1A237E) else Color.Black
                        )
                        Row {
                            repeat(5) { i ->
                                Icon(
                                    Icons.Default.Star, null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (i < comentario.estrellas) Color(0xFFFFB300) else Color.LightGray
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row {
                            if (esMio) {
                                IconButton(onClick = { onEditClick(comentario) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Edit, stringResource(R.string.edit), tint = Color.Blue, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (esMio || esAdmin) {
                                IconButton(onClick = { bookViewModel.deleteComentario(comentario.libro_id, comentario) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (esAdmin) {
                                IconButton(onClick = { bookViewModel.toggleCensura(comentario) }, modifier = Modifier.size(28.dp)) {
                                    Icon(
                                        imageVector = if (comentario.censurado) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (comentario.censurado) stringResource(R.string.uncensor_comment) else stringResource(R.string.censor_comment),
                                        tint = Color.Red,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = formatFecha(comentario.fecha),
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (comentario.censurado && !esAdmin) stringResource(R.string.hidden_content) else comentario.comentario_texto,
                    fontSize = 13.sp,
                    color = if (comentario.censurado) Color.Red else Color.DarkGray,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Formatea una fecha en milisegundos a formato dd/MM/yyyy HH:mm.
 */
fun formatFecha(ms: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(ms))
}

/**
 * Botón inferior fijo para solicitar una reserva.
 */
@Composable
private fun BottomReservarButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(16.dp).height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2A78))
    ) {
        Text(stringResource(R.string.reserve_now), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}