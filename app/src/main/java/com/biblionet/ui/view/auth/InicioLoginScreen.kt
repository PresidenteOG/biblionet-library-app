// Archivo: InicioLoginScreen.kt
package com.biblionet.ui.view.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biblionet.R
import com.biblionet.ui.components.AuthHeader

private val ColorPrimary = Color(0xFF1A237E)
private val ColorBg = Color(0xFFF8F9FA)

@Composable
fun InicioLoginScreen(
    onNavegarLogin: () -> Unit,
    onNavegarRegistro: () -> Unit
) {
    Box(
        modifier = Modifier.
        fillMaxSize().
        background(ColorBg)) {

        // Aumentamos el alto del fondo azul (0.6f) para que quepa el título y subtítulo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .clip(RoundedCornerShape(bottomStart = 60.dp, bottomEnd = 80.dp))
                .background(ColorPrimary)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Reducimos el espacio superior para subir el logo
            Spacer(modifier = Modifier.height(40.dp))

            AuthHeader(
                title = "BiblioNET",
                subtitle = stringResource(R.string.tu_biblioteca_digital)

            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onNavegarLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                shape = CircleShape
            ) {
                Text(stringResource(R.string.iniciar_sesion), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onNavegarRegistro,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorPrimary),
                shape = CircleShape
            ) {
                Text(stringResource(R.string.registrarse), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
        Text(
            text = stringResource(R.string.derechos_reservados),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        )
    }
}
