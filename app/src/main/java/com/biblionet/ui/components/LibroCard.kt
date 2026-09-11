package com.biblionet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.biblionet.R
import com.biblionet.data.model.book.Libro
import coil.compose.AsyncImage

@Composable
fun LibroCard(
    libro: Libro,
    rating: Double,
    isFavorite: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Contenedor de la Imagen
            Box(modifier = Modifier.weight(1f)) {
                AsyncImage(
                    model = libro.imagen_url.ifEmpty { null },
                    contentDescription = libro.titulo,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Badge de Rating
                Surface(
                    modifier = Modifier.padding(8.dp).align(Alignment.TopEnd),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val calificacion = if (libro.calificacion.isNullOrBlank()) "0.0" else libro.calificacion
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                        Text(text = calificacion, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(start = 2.dp))
                    }
                }
            }

            // Info del Libro
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = libro.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = libro.autor,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun InfoPrestamoCard(
    libro: Libro,
    estado: String? = null,
    fecha: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Imagen del libro
            AsyncImage(
                model = libro.imagen_url.ifEmpty { null },
                contentDescription = null,
                modifier = Modifier
                    .size(width = 90.dp, height = 60.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFFF1F4F8)),
                contentScale = ContentScale.Crop
            )

            // 2. Título e ISBN
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = libro.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ISBN: ${libro.isbn}", // Puedes crear un string resource si prefieres: stringResource(R.string.isbn_label, libro.isbn)
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            // 3. Estado (Multi-idioma) y Fecha
            Column(
                horizontalAlignment = Alignment.End
            ) {
                estado?.let { textoEstado ->
                    val estadoLower = textoEstado.lowercase()

                    // Mapeo de Color
                    val colorEstado = when (estadoLower) {
                        "retrasado" -> Color.Red
                        "reservado" -> Color(0xFF4CAF50)
                        "pendiente", "confirmar bibliotecario" -> Color(0xFFFFA000)
                        "activo" -> Color(0xFF1A237E)
                        else -> Color.Gray
                    }

                    // Mapeo de Texto Multi-idioma
                    val textoTraducido = when (estadoLower) {
                        "retrasado" -> stringResource(R.string.status_overdue)
                        "reservado" -> stringResource(R.string.status_reserved)
                        "pendiente", "confirmar bibliotecario" -> stringResource(R.string.status_pending)
                        "activo" -> stringResource(R.string.status_active)
                        "devuelto" -> stringResource(R.string.status_returned)
                        else -> textoEstado // Por si llega algo raro
                    }

                    Text(
                        text = textoTraducido,
                        color = colorEstado,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }

                fecha?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = Color.LightGray.copy(alpha = 0.3f)
        )
    }
}