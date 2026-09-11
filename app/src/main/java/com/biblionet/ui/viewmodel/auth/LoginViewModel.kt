package com.biblionet.ui.viewmodel.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.biblionet.data.model.auth.Role
import com.biblionet.data.model.auth.Usuario
import com.biblionet.utils.GlobalState.usuario


class LoginViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun iniciarSesion(email: String, pass: String, onSuccess: (Role) -> Unit, onError: (String) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            onError("Por favor, rellena todos los campos")
            return
        }

        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""

                // Buscamos el documento del usuario
                db.collection("usuarios").document(uid).get()
                    .addOnSuccessListener { document ->

                        if (document.exists()) {

                            // 1. Convertimos TODO el documento directamente a tu clase Usuario
                            // Firebase mapeará automáticamente "rol" (String en DB) a Role (Enum en Kotlin)
                            val usuarioLogueado = document.toObject(Usuario::class.java)

                            if (usuarioLogueado != null) {
                                // 2. GUARDAR EN EL ESTADO GLOBAL
                                // Esto hace que GlobalState.usuario deje de ser null
                                usuario = usuarioLogueado

                                // 3. Ejecutar éxito pasando el rol del objeto
                                onSuccess(usuarioLogueado.rol)
                            } else {
                                onError("Error al procesar los datos del usuario")
                            }

                        } else {
                            onError("El perfil no existe en la base de datos")
                        }
                    }
                    .addOnFailureListener { exception ->
                        onError("Error de base de datos: ${exception.localizedMessage}")
                    }
            }
            .addOnFailureListener {
                onError("Correo o contraseña incorrectos")
            }
    }

    fun recuperarContrasena(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank()) {
            onError("Introduce tu correo para recibir el enlace")
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError("Error: ${e.localizedMessage}")
            }
    }
}