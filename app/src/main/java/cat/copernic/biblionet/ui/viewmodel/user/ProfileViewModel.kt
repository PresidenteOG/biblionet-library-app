// Archivo: src/main/java/cat/copernic/biblionet/ui/viewmodel/user/ProfileViewModel.kt
package cat.copernic.biblionet.ui.viewmodel.user

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.copernic.biblionet.data.remote.cloudinary.CloudinaryRepository
import cat.copernic.biblionet.data.model.auth.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
private var userListener: com.google.firebase.firestore.ListenerRegistration? = null
private var historialListener: com.google.firebase.firestore.ListenerRegistration? = null
class ProfileViewModel : ViewModel() {

    private val cloudinaryRepository = CloudinaryRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    var usuario by mutableStateOf<Usuario?>(null)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var fotoUrlSubida by mutableStateOf<String?>(null)
        private set

    var librosLeidosCount by mutableStateOf(0)
        private set

    init {
        loadUser()
    }

    private fun loadUser() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("usuarios").document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    usuario = doc.toObject(Usuario::class.java)
                    loadHistorialReservas(uid)
                } else {
                    isLoading = false
                }
            }
    }

    private fun loadHistorialReservas(uid: String) {
        db.collection("prestamos")
            .whereEqualTo("usuario_id", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                // Usamos una corrutina para verificar libro por libro
                viewModelScope.launch {
                    val devueltos = snapshot.documents.filter {
                        it.getString("estado").equals("devuelto", ignoreCase = true)
                    }

                    var contadorReal = 0

                    for (doc in devueltos) {
                        val libroId = doc.getString("libro_id") ?: ""
                        if (libroId.isNotEmpty()) {
                            try {
                                // SOLUCIÓN: Comprobamos si el libro no ha sido borrado de la app
                                // para que coincida exactamente con la pestaña "Historial"
                                val libroDoc = db.collection("libros").document(libroId).get().await()
                                if (libroDoc.exists()) {
                                    contadorReal++
                                }
                            } catch (e: Exception) {
                                // Ignoramos si hay fallo de red, simplemente no lo contamos
                            }
                        }
                    }

                    librosLeidosCount = contadorReal
                    isLoading = false
                }
            }
    }

    fun uploadProfileImage(context: Context, imageUri: Uri, onComplete: (String?) -> Unit) {
        cloudinaryRepository.uploadProfileImage(context, imageUri) { url ->
            if (url != null) {
                fotoUrlSubida = url
            }
            onComplete(url)
        }
    }

    fun actualizarPerfil(nombre: String, telefono: String, fotoUrl: String?) {
        val uid = auth.currentUser?.uid ?: return
        val updates = mutableMapOf<String, Any>("nombre" to nombre, "telefono" to telefono)
        if (!fotoUrl.isNullOrEmpty()) updates["foto_url"] = fotoUrl

        viewModelScope.launch {
            try {
                db.collection("usuarios").document(uid).update(updates).await()
                val comentariosQuery = db.collection("comentarios").whereEqualTo("usuario_id", uid).get().await()

                if (!comentariosQuery.isEmpty) {
                    val batch = db.batch()
                    for (documento in comentariosQuery.documents) {
                        batch.update(documento.reference, "nombre_usuario", nombre)
                        if (!fotoUrl.isNullOrEmpty()) {
                            batch.update(documento.reference, "foto_usuario_url", fotoUrl)
                        }
                    }
                    batch.commit().await()
                }
            } catch (e: Exception) {}
        }
    }

    fun actualizarIdioma(context: Context, nuevoIdiomaCode: String) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("usuarios").document(uid).update("idioma", nuevoIdiomaCode)
        }
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("app_lang", nuevoIdiomaCode).apply()
    }

    fun logout() {
        auth.signOut()
        usuario = null
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        historialListener?.remove()
    }
}