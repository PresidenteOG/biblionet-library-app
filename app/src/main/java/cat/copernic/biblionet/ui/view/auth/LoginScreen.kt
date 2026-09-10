// Archivo: src/main/java/cat/copernic/biblionet/ui/view/auth/LoginScreen.kt
package cat.copernic.biblionet.ui.view.auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cat.copernic.biblionet.R
import cat.copernic.biblionet.data.model.auth.Role
import cat.copernic.biblionet.ui.components.AuthTextField
import cat.copernic.biblionet.ui.viewmodel.auth.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: (Role) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)

    var email by remember { mutableStateOf(prefs.getString("saved_email", "") ?: "") }
    var password by remember { mutableStateOf(prefs.getString("saved_password", "") ?: "") }
    var rememberMe by remember { mutableStateOf(prefs.getBoolean("remember_me", false)) }

    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Botón Volver
        Row(
            modifier = Modifier
                .padding(16.dp)
                .clickable { onBack() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color.Gray
            )
            Text(" " + stringResource(R.string.volver), color = Color.Gray)
        }

        Text(
            stringResource(R.string.bienvenido_nuevo),
            modifier = Modifier.padding(horizontal = 24.dp),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A237E)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Campos de texto
                AuthTextField(email, { email = it; errorText = null }, stringResource(R.string.correo), Icons.Default.Email)
                Spacer(modifier = Modifier.height(16.dp))
                AuthTextField(password, { password = it; errorText = null }, stringResource(R.string.contrasena), Icons.Default.Lock, isPassword = true)

                // Fila de opciones (FIXED: Usando weights para evitar solapamiento)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f) // Este peso empuja al botón de la derecha
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1A237E))
                        )
                        Text(
                            text = stringResource(R.string.recordarme),
                            fontSize = 14.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis // Si es demasiado largo, pone "..."
                        )
                    }

                    val exitoMsg = context.getString(R.string.correo_recuperacion_exito)
                    Text(
                        text = stringResource(R.string.olvidado_contrasena),
                        modifier = Modifier
                            .clickable {
                                viewModel.recuperarContrasena(email,
                                    onSuccess = { errorText = exitoMsg },
                                    onError = { errorText = it }
                                )
                            }
                            .padding(start = 4.dp),
                        color = Color(0xFF1A237E),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Texto de error/éxito
                if (errorText != null) {
                    val color = if (errorText == context.getString(R.string.correo_recuperacion_exito) ||
                        errorText!!.contains("éxito") ||
                        errorText!!.contains("successfully") ||
                        errorText!!.contains("èxit")) Color(0xFF2E7D32)
                    else Color.Red

                    Text(
                        text = errorText!!,
                        color = color,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Botón de Login / Loading
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp)
                    )
                } else {
                    Button(
                        onClick = {
                            loading = true
                            if (rememberMe) {
                                prefs.edit()
                                    .putString("saved_email", email)
                                    .putString("saved_password", password)
                                    .putBoolean("remember_me", true)
                                    .apply()
                            } else {
                                prefs.edit().clear().apply()
                            }

                            viewModel.iniciarSesion(email, password,
                                onSuccess = { rol ->
                                    loading = false
                                    onLoginSuccess(rol)
                                },
                                onError = { error ->
                                    loading = false
                                    errorText = error
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                        shape = CircleShape
                    ) {
                        Text(
                            text = stringResource(R.string.entrar),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}