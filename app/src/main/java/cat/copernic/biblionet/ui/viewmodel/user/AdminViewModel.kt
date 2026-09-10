// Archivo: src/main/java/cat/copernic/biblionet/ui/viewmodel/user/AdminViewModel.kt
package cat.copernic.biblionet.ui.viewmodel.user

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.copernic.biblionet.data.model.auth.Role
import cat.copernic.biblionet.data.model.auth.Usuario
import cat.copernic.biblionet.data.model.library.Biblioteca
import cat.copernic.biblionet.data.model.book.Libro
import cat.copernic.biblionet.data.remote.cloudinary.CloudinaryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val cloudinaryRepository = CloudinaryRepository() // REPOSITORIO DE IMÁGENES AÑADIDO

    // --- Estados Observables ---
    var usuarios by mutableStateOf<List<Usuario>>(emptyList())
        private set
    var bibliotecas by mutableStateOf<List<Biblioteca>>(emptyList())
        private set
    var librosCount by mutableStateOf(0)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var selectedUid by mutableStateOf<String?>(null)
        private set
    var selectedRole by mutableStateOf<String?>(null)
        private set
    var selectedBibliotecaId by mutableStateOf("")
        private set

    val filteredUsers: List<Usuario>
        get() = usuarios.filter {
            it.nombre.contains(searchQuery, ignoreCase = true) ||
                    it.email.contains(searchQuery, ignoreCase = true)
        }

    private var userListener: ListenerRegistration? = null
    private var bookListener: ListenerRegistration? = null
    private var bibliotecaListener: ListenerRegistration? = null

    init { fetchAdminData() }

    private fun fetchAdminData() {
        userListener = db.collection("usuarios").addSnapshotListener { result, error ->
            if (error != null) return@addSnapshotListener
            usuarios = result?.documents?.mapNotNull { doc ->
                try {
                    val u = doc.toObject(Usuario::class.java)
                    u?.copy(uid = doc.id)
                } catch (e: Exception) { null }
            } ?: emptyList()
        }

        bookListener = db.collection("libros").addSnapshotListener { result, error ->
            if (error == null && result != null) { librosCount = result.size() }
        }

        bibliotecaListener = db.collection("bibliotecas").addSnapshotListener { result, error ->
            if (error == null && result != null) {
                bibliotecas = result.documents.mapNotNull { doc ->
                    try {
                        val b = doc.toObject(Biblioteca::class.java)
                        b?.copy(id = doc.id)
                    } catch (e: Exception) { null }
                } ?: emptyList()
            }
        }
    }

    fun onSearchQueryChange(query: String) { searchQuery = query }

    fun selectUser(uid: String) {
        selectedUid = uid
        val user = usuarios.find { it.uid == uid }
        selectedRole = user?.rol?.name
        selectedBibliotecaId = user?.biblioteca_id ?: ""
    }

    fun selectRole(role: String) {
        selectedRole = role
        if (role != Role.LIBRARIAN.name) selectedBibliotecaId = ""
    }

    fun onBibliotecaSelected(id: String) { selectedBibliotecaId = id }

    fun saveRoleChange(onSuccess: () -> Unit) {
        val uid = selectedUid ?: return
        val roleName = selectedRole ?: return
        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any?>(
                    "rol" to roleName,
                    "biblioteca_id" to if (roleName == Role.LIBRARIAN.name) selectedBibliotecaId else "",
                    "forceLogout" to true
                )
                db.collection("usuarios").document(uid).update(updates).await()
                onSuccess()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun reiniciarQuizGlobalmente(afectarRachas: Boolean, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val usuariosSnapshot = db.collection("usuarios").get().await()
                val batch = db.batch()

                val hoyStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val ayerStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.time)

                for (doc in usuariosSnapshot.documents) {
                    val updates = mutableMapOf<String, Any>("ultima_partida_str" to "")
                    if (afectarRachas) {
                        val ultimoDiaRacha = doc.getString("ultimo_dia_racha")
                        if (ultimoDiaRacha == hoyStr) {
                            updates["ultimo_dia_racha"] = ayerStr
                        }
                    }
                    batch.set(doc.reference, updates, SetOptions.merge())
                }
                batch.commit().await()

                db.collection("sistema").document("alertas").set(
                    mapOf(
                        "mensaje" to "¡El administrador ha reiniciado el Quiz Diario! Juega ahora.",
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                ).await()

                onComplete(true, "OK")
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.localizedMessage ?: "Error desconocido")
            }
        }
    }

    // --- FUNCIÓN AÑADIDA PARA SUBIR FOTOS DE BIBLIOTECAS ---
    fun uploadLibraryImage(context: Context, uri: Uri, onResult: (String?) -> Unit) {
        cloudinaryRepository.uploadProfileImage(context, uri) { url ->
            onResult(url)
        }
    }

    val librosEjemplo = listOf(
        Libro(
            titulo = "Código Limpio", autor = "Robert C. Martin", categoria_id = "programacion",
            isbn = "9780132350884", imagen_url = "https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg",
            descripcion_corta = "El manual esencial para escribir software de alta calidad.",
            calificacion = "0.0", idioma = "ES"
        ),
        Libro(
            titulo = "El Programador Pragmático", autor = "Andrew Hunt", categoria_id = "programacion",
            isbn = "9780135957059", imagen_url = "https://covers.openlibrary.org/b/isbn/9780135957059-L.jpg",
            descripcion_corta = "Consejos prácticos para mejorar tu carrera como desarrollador.",
            calificacion = "0.0", idioma = "ES"
        ),
        Libro(
            titulo = "Kotlin en Acción", autor = "Dmitry Jemerov", categoria_id = "programacion",
            isbn = "9781617293290", imagen_url = "https://covers.openlibrary.org/b/isbn/9781617293290-L.jpg",
            descripcion_corta = "La guía definitiva para desarrollar aplicaciones con Kotlin.",
            calificacion = "0.0", idioma = "EN"
        ),
        Libro(
            titulo = "JavaScript Elocuente", autor = "Marijn Haverbeke", categoria_id = "programacion",
            isbn = "9781593279509", imagen_url = "https://covers.openlibrary.org/b/isbn/9781593279509-L.jpg",
            descripcion_corta = "Una inmersión profunda en el lenguaje más usado de la web.",
            calificacion = "0.0", idioma = "ES"
        ),
        Libro(
            titulo = "Refactorización", autor = "Martin Fowler", categoria_id = "programacion",
            isbn = "9780134757599", imagen_url = "https://covers.openlibrary.org/b/isbn/9780134757599-L.jpg",
            descripcion_corta = "Mejora el diseño de tu código existente sin cambiar su comportamiento.",
            calificacion = "0.0", idioma = "ES"
        ),
        Libro(
            titulo = "La La Land", autor = "Damien Chazelle", categoria_id = "cinema",
            isbn = "9781495088223", imagen_url = "https://covers.openlibrary.org/b/isbn/9781495088223-L.jpg",
            descripcion_corta = "Una carta de amor al cine musical clásico de Hollywood.",
            calificacion = "0.0", idioma = "EN"
        ),
        Libro(
            titulo = "El Guion", autor = "Robert McKee", categoria_id = "cinema",
            isbn = "9780060391683", imagen_url = "https://covers.openlibrary.org/b/isbn/9780060391683-L.jpg",
            descripcion_corta = "La biblia de los guionistas sobre estructura y estilo.",
            calificacion = "0.0", idioma = "ES"
        ),
        Libro(
            titulo = "Así se hacen las películas", autor = "Sidney Lumet", categoria_id = "cinema",
            isbn = "9780679756606", imagen_url = "https://covers.openlibrary.org/b/isbn/9780679756606-L.jpg",
            descripcion_corta = "Memorias y lecciones de uno de los grandes directores.",
            calificacion = "0.0", idioma = "ES"
        ),
        Libro(
            titulo = "Meditaciones", autor = "Marco Aurelio", categoria_id = "filosofia",
            isbn = "9788470305269", imagen_url = "https://covers.openlibrary.org/b/isbn/9788470305269-L.jpg",
            descripcion_corta = "Pensamientos estoicos sobre la vida, la muerte y el deber.",
            calificacion = "0.0", idioma = "ES"
        ),
        Libro(
            titulo = "Así habló Zarathustra", autor = "Friedrich Nietzsche", categoria_id = "filosofia",
            isbn = "9788420650913", imagen_url = "https://covers.openlibrary.org/b/isbn/9788420650913-L.jpg",
            descripcion_corta = "Una obra maestra sobre el superhombre y la voluntad de poder.",
            calificacion = "0.0", idioma = "ES"
        ),
        Libro(
            titulo = "El mundo de Sofía", autor = "Jostein Gaarder", categoria_id = "filosofia",
            isbn = "9788478441464", imagen_url = "https://covers.openlibrary.org/b/isbn/9788478441464-L.jpg",
            descripcion_corta = "Una novela fascinante sobre la historia de la filosofía.",
            calificacion = "0.0", idioma = "ES"
        ),
        Libro(
            titulo = "El mito de Sísifo", autor = "Albert Camus", categoria_id = "filosofia",
            isbn = "9788420651156", imagen_url = "https://covers.openlibrary.org/b/isbn/9788420651156-L.jpg",
            descripcion_corta = "Ensayo sobre el absurdo y la condición humana.",
            calificacion = "0.0", idioma = "ES"
        )
    )

    fun seedLibros() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "admin_id"
        viewModelScope.launch {
            try {
                librosEjemplo.forEach { libro ->
                    val docRef = db.collection("libros").document()
                    val libroFinal = libro.copy(
                        libro_id = docRef.id,
                        creado_por = uid,
                        fecha_creacion = Timestamp.now(),
                        descripcion_larga = "Esta es una descripción detallada de prueba para el libro ${libro.titulo}."
                    )
                    docRef.set(libroFinal).await()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        bookListener?.remove()
        bibliotecaListener?.remove()
    }
}