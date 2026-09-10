package cat.copernic.biblionet.data.repository.auth

import cat.copernic.biblionet.data.model.auth.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio encargado de gestionar operaciones relacionadas con usuarios y autenticación.
 *
 * Este repositorio actúa como intermediario entre Firebase Authentication y Firestore,
 * ofreciendo funciones para login, observación de usuarios, actualización de perfiles
 * y manejo del estado de sesión.
 *
 * @property auth Instancia de FirebaseAuth para gestionar la autenticación.
 * @property db Instancia de FirebaseFirestore para acceder a los datos de los usuarios.
 */
class UsuarioRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Inicia sesión con correo electrónico y contraseña.
     *
     * @param email Correo electrónico del usuario.
     * @param pass Contraseña del usuario.
     * @return `true` si la autenticación fue exitosa, `false` en caso de error.
     */
    suspend fun login(email: String, pass: String): Boolean {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Observa los cambios en los datos de un usuario en Firestore.
     *
     * Este método devuelve un [Flow] que emite la información del usuario
     * cada vez que cambia en la base de datos.
     *
     * @param uid Identificador del usuario a observar.
     * @return Flow que emite instancias de [Usuario] o `null` si no existe.
     */
    fun observeUsuario(uid: String): Flow<Usuario?> = callbackFlow {
        val listener = db.collection("usuarios")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(Usuario::class.java)
                trySend(user)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Obtiene el nombre de un usuario a partir de su UID.
     *
     * @param uid Identificador del usuario.
     * @return Nombre del usuario, o "Lector de BiblioNet" si no se encuentra.
     */
    suspend fun getNombreUsuario(uid: String): String {
        return try {
            val doc = db.collection("usuarios").document(uid).get().await()
            doc.getString("nombre") ?: "Lector de BiblioNet"
        } catch (e: Exception) {
            "Lector de BiblioNet"
        }
    }

    /**
     * Obtiene la URL de la foto de perfil de un usuario.
     *
     * @param uid Identificador del usuario.
     * @return URL de la foto de perfil, o una URL por defecto si no existe.
     */
    suspend fun getFotoUsuario(uid: String): String {
        return try {
            val doc = db.collection("usuarios").document(uid).get().await()
            doc.getString("foto_url") ?: "https://res.cloudinary.com/die6u09pk/image/upload/v1771956640/bxazkjut72hrw915jbey.jpg"
        } catch (e: Exception) {
            "https://res.cloudinary.com/die6u09pk/image/upload/v1771956640/bxazkjut72hrw915jbey.jpg"
        }
    }

    /**
     * Actualiza los datos del perfil de un usuario.
     *
     * @param uid Identificador del usuario.
     * @param updates Mapa con los campos a actualizar y sus nuevos valores.
     * @return Resultado de la operación, exitoso o con fallo.
     */
    suspend fun actualizarPerfil(uid: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            db.collection("usuarios").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resetea el flag de forceLogout de un usuario.
     *
     * Esto puede ser útil para restaurar la sesión después de forzar un cierre de sesión.
     *
     * @param uid Identificador del usuario.
     */
    suspend fun resetForceLogout(uid: String) {
        try {
            db.collection("usuarios").document(uid).update("forceLogout", false).await()
        } catch (e: Exception) {
            // Log error
        }
    }

    /**
     * Cierra la sesión del usuario actual.
     */
    fun logout() {
        auth.signOut()
    }

    /**
     * Obtiene el UID del usuario actualmente autenticado.
     *
     * @return UID del usuario o `null` si no hay usuario autenticado.
     */
    fun getCurrentUid(): String? = auth.currentUser?.uid

    /**
     * Añade un listener al estado de autenticación.
     *
     * @param listener Listener de FirebaseAuth para cambios en el estado de sesión.
     */
    fun addAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.addAuthStateListener(listener)
    }

    /**
     * Remueve un listener previamente añadido al estado de autenticación.
     *
     * @param listener Listener de FirebaseAuth que se desea eliminar.
     */
    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }
}