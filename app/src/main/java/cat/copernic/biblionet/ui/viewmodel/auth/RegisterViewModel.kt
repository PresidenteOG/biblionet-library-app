// Archivo: RegisterViewModel.kt
package cat.copernic.biblionet.ui.viewmodel.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import cat.copernic.biblionet.data.model.auth.Role
import cat.copernic.biblionet.data.model.auth.Usuario
import cat.copernic.biblionet.utils.GlobalState.usuario
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import cat.copernic.biblionet.R

class RegisterViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun registrarUsuario(
        nombre: String,
        email: String,
        telefono: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        // 1. Validar campos vacíos
        if (nombre.isBlank() || email.isBlank() || pass.isBlank() || telefono.isBlank()) {
            onError(R.string.error_campos_vacios, null)
            return
        }

        // 2. Validar formato de Email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            onError(R.string.error_email_invalido, null)
            return
        }

        // 3. Validar formato de Teléfono (mínimo 9 dígitos para España/general)
        // Usamos una expresión regular simple o Patterns.PHONE
        if (telefono.length < 9 || !Patterns.PHONE.matcher(telefono).matches()) {
            onError(R.string.error_telefono_invalido, null)
            return
        }

        // 4. Validar longitud de contraseña (seguridad mínima)
        if (pass.length < 6) {
            onError(R.string.error_pass_corta, null)
            return
        }
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    onError(R.string.error_uid, null)
                    return@addOnSuccessListener
                }

                // SOLUCIÓN: Detectamos el idioma actual del sistema/app al crear la cuenta.
                // Si es catalán o inglés lo guarda; si no, asume español por defecto.
                val langCode = Locale.getDefault().language
                val idiomaDetectado = if (langCode == "ca" || langCode == "en") langCode else "es"

                val nuevoUsuario = Usuario(
                    uid = uid,
                    nombre = nombre,
                    email = email,
                    foto_url = "https://res.cloudinary.com/die6u09pk/image/upload/v1771956640/bxazkjut72hrw915jbey.jpg",
                    rol = Role.READER,
                    fecha_registro = Timestamp.now(),
                    puntos_totales = 0,
                    telefono = telefono,
                    biblioteca_id = "",
                    favoritos = emptyList(),
                    ultima_partida = null,
                    idioma = idiomaDetectado // ¡Ahora se guarda en Firebase correctamente!
                )

                db.collection("usuarios").document(uid).set(nuevoUsuario)
                    .addOnSuccessListener {
                        // 2. GUARDAR EN EL ESTADO GLOBAL
                        // Esto hace que GlobalState.usuario deje de ser null
                        usuario = nuevoUsuario
                        onSuccess()

                    }
                    .addOnFailureListener { e ->
                        auth.currentUser?.delete()
                        onError(R.string.error_guardar_datos, e.message)
                    }
            }
            .addOnFailureListener { e ->
                onError(R.string.error_registro, e.message)
            }
    }
}