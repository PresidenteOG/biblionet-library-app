// Archivo: RegisterScreen.kt
package com.biblionet.ui.view.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.biblionet.R
import com.biblionet.ui.components.AuthTextField
import com.biblionet.ui.viewmodel.auth.RegisterViewModel

@Composable
fun RegistroScreen(
    viewModel: RegisterViewModel = viewModel(),
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFFF8F9FA))
        .statusBarsPadding()
        .navigationBarsPadding()) {

        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.volver), tint = Color(0xFF1A237E))
        }

        Text(
            stringResource(R.string.crear_cuenta),
            modifier = Modifier.padding(horizontal = 24.dp),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A237E)
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp),
            shape = RoundedCornerShape(32.dp, 32.dp, 0.dp, 0.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                AuthTextField(nombre, { nombre = it }, stringResource(R.string.nombre_completo), Icons.Default.Person)
                Spacer(modifier = Modifier.height(16.dp))

                AuthTextField(email, { email = it }, stringResource(R.string.email), Icons.Default.Email)
                Spacer(modifier = Modifier.height(16.dp))

                AuthTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = stringResource(R.string.telefono),
                    icon = Icons.Default.Phone
                )

                Spacer(modifier = Modifier.height(16.dp))
                AuthTextField(password, { password = it }, stringResource(R.string.contrasena), Icons.Default.Lock, isPassword = true)

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (loading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF1A237E))
                    }
                } else {
                    Button(
                        onClick = {
                            loading = true
                            errorMessage = null
                            viewModel.registrarUsuario(nombre, email, telefono, password,
                                onSuccess = {
                                    loading = false
                                    onRegisterSuccess()
                                },
                                onError = { resId, errorTecnico ->
                                    loading = false

                                    // Obtenemos el mensaje traducido usando el ID que nos da el ViewModel
                                    errorMessage = if (errorTecnico != null) {
                                        // Si hay un %1$s en el XML, pasamos el errorTecnico como argumento
                                        context.getString(resId, errorTecnico)
                                    } else {
                                        // Si es un error simple (como campos vacíos), solo obtenemos el string
                                        context.getString(resId)
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                        shape = CircleShape
                    ) {
                        Text(stringResource(R.string.registrarse), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}