package cat.copernic.biblionet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ESTOS SON LOS DE COIL (Si salen en rojo, el Sync de Gradle falló)
import coil.compose.AsyncImage
import coil.request.ImageRequest

// IMPORT DE TU MODELO (Asegúrate de que la ruta es correcta)
import cat.copernic.biblionet.data.model.auth.Usuario
@Composable
fun HeaderSection(usuario: Usuario) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Inicio", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
            Text("Hola, ${usuario.nombre}", fontSize = 14.sp, color = Color.Gray)
        }

        // Imagen de perfil usando Coil
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(usuario.foto_url)
                .crossfade(true)
                .build(),
            contentDescription = "Foto de perfil",
            modifier = Modifier
                .size(45.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentScale = ContentScale.Crop
        )
    }
}