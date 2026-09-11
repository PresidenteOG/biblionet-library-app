// Archivo: EditProfileScreen.kt
package com.biblionet.ui.view.profile

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.biblionet.R
import com.biblionet.data.model.auth.Usuario
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biblionet.ui.viewmodel.user.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPerfilScreen(
    usuario: Usuario,
    onBack: () -> Unit,
    onSave: (String, String, String?) -> Unit
) {
    val context = LocalContext.current
    val profileViewModel: ProfileViewModel = viewModel()

    var nombre by remember { mutableStateOf(usuario.nombre) }
    var tele by remember { mutableStateOf(usuario.telefono ?: "") }
    var fotoUrl by remember { mutableStateOf(usuario.foto_url) }
    var isUploading by remember { mutableStateOf(false) }

    val errorSubir = context.getString(R.string.error_subiendo_imagen)
    val permisoFotos = context.getString(R.string.permiso_fotos_necesario)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploading = true
            profileViewModel.uploadProfileImage(context, it) { url ->
                if (url != null) {
                    fotoUrl = url
                } else {
                    Toast.makeText(context, errorSubir, Toast.LENGTH_SHORT).show()
                }
                isUploading = false
            }
        }
    }

    val readImagesPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launcher.launch("image/*")
        } else {
            Toast.makeText(context, permisoFotos, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.editar_perfil), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.volver))
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(4.dp))

            Box(contentAlignment = Alignment.BottomEnd) {

                AsyncImage(
                    model = fotoUrl,
                    contentDescription = stringResource(R.string.foto_perfil),
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = Color(0xFF1A237E),
                            shape = CircleShape
                        )
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )

                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.BottomEnd),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }

                IconButton(
                    onClick = {
                        readImagesPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF1A237E), CircleShape)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.cambiar_foto),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text(stringResource(R.string.nombre)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = tele,
                onValueChange = { tele = it },
                label = { Text(stringResource(R.string.telefono)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email Display (Text Only)
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text(
                    text = stringResource(R.string.email),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = usuario.email,
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Points Display (Text Only)
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text(
                    text = stringResource(R.string.puntos_totales),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = usuario.puntos_totales.toString(),
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onSave(nombre, tele, fotoUrl) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A237E),
                    contentColor = Color.White
                ),
                enabled = !isUploading
            ) {
                Text(stringResource(R.string.guardar_cambios), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}
