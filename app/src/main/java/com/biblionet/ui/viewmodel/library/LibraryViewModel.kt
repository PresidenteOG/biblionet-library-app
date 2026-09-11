package com.biblionet.ui.viewmodel.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biblionet.R
import com.biblionet.data.model.library.Biblioteca
import com.biblionet.data.remote.cloudinary.CloudinaryRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LibraryUiState(
    val id: String? = null,
    val nombre: String = "",
    val direccion: String = "",
    val email: String = "",
    val telefono: String = "",
    val descripcion: String = "",
    val fotoUrl: String = "https://res.cloudinary.com/die6u09pk/image/upload/v1773084608/qvdtbx4wx0zwut2b1ymw.jpg",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val accesibilidad: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorResId: Int? = null,
    val errorTecnico: String? = null
)

class LibraryViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val cloudinaryRepository = CloudinaryRepository()

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    // Búsqueda
    private val _todasLasBibliotecas = MutableStateFlow<List<Biblioteca>>(emptyList())
    private val _searchQuery = MutableStateFlow("")

    val bibliotecasFiltradas: StateFlow<List<Biblioteca>> = combine(
        _todasLasBibliotecas,
        _searchQuery
    ) { bibliotecas, query ->
        if (query.isBlank()) emptyList()
        else bibliotecas.filter {
            it.nombre.contains(query, ignoreCase = true) ||
                    it.direccion.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchTodasLasBibliotecas()
    }

    private fun fetchTodasLasBibliotecas() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("bibliotecas").get().await()
                _todasLasBibliotecas.value = snapshot.toObjects(Biblioteca::class.java)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorResId = R.string.error_prefix, errorTecnico = e.localizedMessage) }
            }
        }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    // Actualización de campos
    fun onNombreChange(v: String) = _uiState.update { it.copy(nombre = v) }
    fun onDireccionChange(v: String) = _uiState.update { it.copy(direccion = v) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v) }
    fun onTelefonoChange(v: String) = _uiState.update { it.copy(telefono = v) }
    fun onDescripcionChange(v: String) = _uiState.update { it.copy(descripcion = v) }
    fun onLatitudChange(v: Double) = _uiState.update { it.copy(latitud = v) }
    fun onLongitudChange(v: Double) = _uiState.update { it.copy(longitud = v) }
    fun onAccesibilidadChange(v: Boolean) = _uiState.update { it.copy(accesibilidad = v) }
    fun resetSuccess() = _uiState.update { it.copy(isSuccess = false) }

    fun uploadLibraryImage(context: Context, uri: Uri, onResult: (String?) -> Unit) {
        cloudinaryRepository.uploadProfileImage(context, uri) { url ->
            if (url != null) {
                _uiState.update { it.copy(fotoUrl = url, errorResId = null) }
                onResult(url)
            } else {
                _uiState.update { it.copy(errorResId = R.string.error_upload_cloudinary) }
                onResult(null)
            }
        }
    }

    fun loadBiblioteca(id: String) {
        if (id.isEmpty()) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val doc = db.collection("bibliotecas").document(id).get().await()
                doc.toObject(Biblioteca::class.java)?.let { b ->
                    _uiState.update { it.copy(
                        id = id, nombre = b.nombre,
                        direccion = b.direccion,
                        email = b.email,
                        telefono = b.telefono,
                        descripcion = b.descripcion,
                        fotoUrl = b.fotoUrl,
                        latitud = b.latitud, longitud = b.longitud,
                        accesibilidad = b.accesibilidad, isLoading = false,
                        errorResId = null
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorResId = R.string.error_prefix, errorTecnico = e.localizedMessage) }
            }
        }
    }

    fun updateBiblioteca(id: String, fotoNueva: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val data = mapOf(
                    "nombre" to state.nombre, "direccion" to state.direccion,
                    "email" to state.email, "telefono" to state.telefono,
                    "descripcion" to state.descripcion, "fotoUrl" to fotoNueva.ifEmpty { state.fotoUrl },
                    "latitud" to state.latitud, "longitud" to state.longitud,
                    "accesibilidad" to state.accesibilidad
                )
                db.collection("bibliotecas").document(id).update(data).await()
                fetchTodasLasBibliotecas() // Refrescar lista para el buscador
                _uiState.update { it.copy(isLoading = false, isSuccess = true, errorResId = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorResId = R.string.error_prefix, errorTecnico = e.localizedMessage) }
            }
        }
    }
    fun addBiblioteca() {
        val state = _uiState.value
        if (state.nombre.isBlank() || state.direccion.isBlank()) {
            _uiState.update { it.copy(errorResId = R.string.error_campos_vacios) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val newDocRef = db.collection("bibliotecas").document()
                val nuevaBiblioteca = Biblioteca(
                    id = newDocRef.id,
                    nombre = state.nombre,
                    direccion = state.direccion,
                    email = state.email,
                    telefono = state.telefono,
                    descripcion = state.descripcion,
                    fotoUrl = state.fotoUrl,
                    latitud = state.latitud,
                    longitud = state.longitud,
                    accesibilidad = state.accesibilidad,
                    horario = emptyMap() // O el valor por defecto que use tu modelo
                )
                newDocRef.set(nuevaBiblioteca).await()
                fetchTodasLasBibliotecas() // Para que aparezca en el buscador
                _uiState.update { it.copy(isLoading = false, isSuccess = true, errorResId = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorResId = R.string.error_prefix, errorTecnico = e.localizedMessage) }
            }
        }
    }
}
