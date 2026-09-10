// Archivo: cat/copernic/biblionet/ui/viewmodel/book/BookViewModel.kt
package cat.copernic.biblionet.ui.viewmodel.book

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.copernic.biblionet.R
import cat.copernic.biblionet.data.model.book.Libro
import cat.copernic.biblionet.data.model.book.Categoria
import cat.copernic.biblionet.data.model.book.Comentario
import cat.copernic.biblionet.data.model.library.Biblioteca
import cat.copernic.biblionet.data.model.transaction.Prestamo
import cat.copernic.biblionet.data.model.inventory.Inventario // IMPORT AÑADIDO
import cat.copernic.biblionet.data.remote.cloudinary.CloudinaryRepository
import cat.copernic.biblionet.data.repository.book.CategoriaRepository // IMPORT CORREGIDO
import cat.copernic.biblionet.data.repository.book.LibroRepository
import cat.copernic.biblionet.data.repository.book.ComentarioRepository
import cat.copernic.biblionet.data.repository.inventory.InventarioRepository
import cat.copernic.biblionet.data.repository.inventory.BibliotecaRepository
import cat.copernic.biblionet.data.repository.transaction.PrestamoRepository
import cat.copernic.biblionet.utils.GlobalState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class BookUiState(
    val libro_id: String? = null,
    val titulo: String = "",
    val edit: String = "",
    val autor: String = "",
    val isbn: String = "",
    val descripcion_corta: String = "",
    val descripcion_larga: String = "",
    val imagen_url: String = "https://res.cloudinary.com/die6u09pk/image/upload/v1773084130/ddqcykw54zndub6gzcka.jpg",
    val categoria_id: String = "",
    val idioma: String = "",
    val calificacion: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isFavorite: Boolean = false,
    val comentarios: List<Comentario> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val error: String? = null,
    val inventories: List<Inventario> = emptyList(),
    val librariesMap: Map<String, Biblioteca> = emptyMap(),
    val isLoadingInventario: Boolean = false
)

class BookViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val cloudinaryRepository = CloudinaryRepository()
    private val categoriaRepository = CategoriaRepository(firestore)
    private val libroRepository = LibroRepository(firestore)
    private val comentarioRepository = ComentarioRepository(firestore)
    private val inventarioRepository = InventarioRepository(firestore)
    private val bibliotecaRepository = BibliotecaRepository(firestore)
    private val prestamoRepository = PrestamoRepository(firestore)

    private val _uiState = MutableStateFlow(BookUiState())
    val uiState: StateFlow<BookUiState> = _uiState.asStateFlow()

    private val _libroNombres = MutableStateFlow<Map<String, String>>(emptyMap())
    val libroNombres: StateFlow<Map<String, String>> = _libroNombres.asStateFlow()

    private val _nombreBibliotecaUsuario = MutableStateFlow("loading")
    val nombreBibliotecaUsuario: StateFlow<String> = _nombreBibliotecaUsuario.asStateFlow()

    init {
        observarCategorias()
    }

    private fun observarCategorias() {
        viewModelScope.launch {
            categoriaRepository.getCategorias().collect { lista ->
                _uiState.update { it.copy(categorias = lista) }
            }
        }
    }

    fun onTituloChange(v: String) = _uiState.update { it.copy(titulo = v) }
    fun onAutorChange(v: String) = _uiState.update { it.copy(autor = v) }

    fun onEditChange(v: String) = _uiState.update { it.copy(autor = v) }
    fun onIsbnChange(v: String) = _uiState.update { it.copy(isbn = v) }
    fun onIdiomaChange(v: String) = _uiState.update { it.copy(idioma = v) }
    fun onDescCortaChange(v: String) = _uiState.update { it.copy(descripcion_corta = v) }
    fun onDescLargaChange(v: String) = _uiState.update { it.copy(descripcion_larga = v) }
    fun onCategoriaChange(v: String) = _uiState.update { it.copy(categoria_id = v) }

    fun resetSuccess() = _uiState.update { it.copy(isSuccess = false) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun uploadBookImage(context: Context, uri: Uri, onResult: (String?) -> Unit) {
        cloudinaryRepository.uploadProfileImage(context, uri) { url ->
            if (url != null) {
                _uiState.update { it.copy(imagen_url = url) }
                onResult(url)
            } else {
                _uiState.update { it.copy(error = "error_upload_cover") }
                onResult(null)
            }
        }
    }

    fun saveLibro() {
        val state = _uiState.value
        if (state.edit.isBlank() ||state.titulo.isBlank() || state.isbn.isBlank() || state.autor.isBlank() || state.categoria_id.isBlank()) {
            _uiState.update { it.copy(error = "error_required_fields") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val nuevoLibro = Libro(
                titulo = state.titulo,
                autor = state.autor,
                isbn = state.isbn,
                idioma = state.idioma,
                descripcion_corta = state.descripcion_corta,
                descripcion_larga = state.descripcion_larga,
                imagen_url = state.imagen_url,
                categoria_id = state.categoria_id,
                fecha_creacion = Timestamp.now(),
                creado_por = auth.currentUser?.uid ?: "anonimo"
            )
            libroRepository.createLibro(nuevoLibro).onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun loadLibro(id: String) {
        if (id.isEmpty()) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            libroRepository.observeLibro(id).collect { libro ->
                libro?.let { l ->
                    _uiState.update { it.copy(
                        libro_id = id,
                        titulo = l.titulo,
                        autor = l.autor,
                        isbn = l.isbn,
                        idioma = l.idioma,
                        descripcion_corta = l.descripcion_corta,
                        descripcion_larga = l.descripcion_larga,
                        imagen_url = l.imagen_url,
                        categoria_id = l.categoria_id,
                        calificacion = l.calificacion.ifBlank { "0.0" },
                        isLoading = false
                    ) }
                }
            }
        }
        checkIfIsFavorite(id)
        loadComentarios(id)
    }

    fun updateLibro(id: String, fotoNueva: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val state = _uiState.value
            val data = mapOf(
                "titulo" to state.titulo,
                "autor" to state.autor,
                "isbn" to state.isbn,
                "idioma" to state.idioma,
                "descripcion_corta" to state.descripcion_corta,
                "descripcion_larga" to state.descripcion_larga,
                "imagen_url" to fotoNueva.ifEmpty { state.imagen_url },
                "categoria_id" to state.categoria_id
            )
            libroRepository.updateLibro(id, data).onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun checkIfIsFavorite(libroId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val isFav = libroRepository.isFavorite(uid, libroId)
            _uiState.update { it.copy(isFavorite = isFav) }
        }
    }

    fun toggleFavorite(libroId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val isNowFav = libroRepository.toggleFavorite(uid, libroId)
            _uiState.update { it.copy(isFavorite = isNowFav) }
        }
    }

    fun addComentario(libroId: String, texto: String, estrellas: Int) {
        val user = auth.currentUser ?: return
        val nuevoId = FirebaseFirestore.getInstance().collection("comentarios").document().id
        val nuevo = Comentario(
            comentario_id = nuevoId,
            libro_id = libroId,
            usuario_id = user.uid,
            comentario_texto = texto,
            estrellas = estrellas
        )
        viewModelScope.launch {
            comentarioRepository.addComentario(nuevo).onSuccess {
                loadComentarios(libroId)
                recalculateAndSyncRating(libroId)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun loadComentarios(libroId: String) {
        viewModelScope.launch {
            val list = comentarioRepository.getComentarios(libroId)
            _uiState.update { it.copy(comentarios = list) }
        }
    }

    fun updateComentario(libroId: String, comentario: Comentario, nuevoTexto: String, nuevasEstrellas: Int) {
        viewModelScope.launch {
            comentarioRepository.updateComentario(comentario, nuevoTexto, nuevasEstrellas).onSuccess {
                loadComentarios(libroId)
                recalculateAndSyncRating(libroId)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteComentario(libroId: String, comentario: Comentario) {
        viewModelScope.launch {
            comentarioRepository.deleteComentario(comentario).onSuccess {
                loadComentarios(libroId)
                recalculateAndSyncRating(libroId)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun toggleCensura(comentario: Comentario) {
        if (GlobalState.usuario?.rol != cat.copernic.biblionet.data.model.auth.Role.ADMIN) {
            _uiState.update { it.copy(error = "error_admin_required") }
            return
        }
        viewModelScope.launch {
            comentarioRepository.toggleCensura(comentario).onSuccess {
                loadComentarios(comentario.libro_id)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    private fun recalculateAndSyncRating(libroId: String) {
        viewModelScope.launch {
            val comments = comentarioRepository.getComentarios(libroId)
            val average = if (comments.isEmpty()) 0.0 else comments.map { it.estrellas }.average()
            val formattedRating = "%.1f".format(average)
            libroRepository.updateLibroRating(libroId, formattedRating, average)
        }
    }

    fun cargarBibliotecasConStock(libroId: String) {
        if (libroId.isEmpty()) return
        _uiState.update { it.copy(isLoadingInventario = true) }
        viewModelScope.launch {
            val inventories = inventarioRepository.getInventarioByLibro(libroId)
                .filter { it.stock_disponible > 0 || it.numero_copias > 0 }

            if (inventories.isEmpty()) {
                _uiState.update { it.copy(isLoadingInventario = false, inventories = emptyList()) }
                return@launch
            }

            val bibIds = inventories.map { it.biblioteca_id }.filter { it.isNotEmpty() }.distinct()
            val bibMap = bibliotecaRepository.getBibliotecasByIds(bibIds)

            _uiState.update { it.copy(
                inventories = inventories,
                librariesMap = bibMap,
                isLoadingInventario = false
            )}
        }
    }

    fun crearInventario(item: Inventario, onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            if (inventarioRepository.existsInBiblioteca(item.libro_id, item.biblioteca_id)) {
                _uiState.update { it.copy(isLoading = false, error = "error_book_exists") }
            } else {
                inventarioRepository.createInventario(item).onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    onSuccess()
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = "error_prefix") }
                }
            }
        }
    }

    fun actualizarInventario(id: String, nuevoStockTotal: Int, nuevoPasillo: String) {
        // 1. Buscamos el ítem actual
        val itemActual = _uiState.value.inventories.find { it.id == id } ?: return

        // 2. VALIDACIÓN DE SEGURIDAD:
        // Si el usuario intenta poner 0 (o reducir el stock) pero hay libros prestados,
        // lanzamos un error y no continuamos.
        val hayLibrosPrestados = itemActual.numero_copias != itemActual.stock_disponible

        if (nuevoStockTotal == 0 && hayLibrosPrestados) {
            _uiState.update { it.copy(
                error = "No puedes eliminar el stock total porque hay libros prestados."
            ) }
            return // Cortamos la ejecución aquí
        }

        // 3. Si la validación pasa, calculamos la diferencia normal
        val diferencia = nuevoStockTotal - itemActual.numero_copias
        val nuevoDisponible = (itemActual.stock_disponible + diferencia).coerceAtLeast(0)

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val updates = mapOf(
                "numero_copias" to nuevoStockTotal,
                "stock_disponible" to nuevoDisponible,
                "pasilloEstanteria" to nuevoPasillo,
                "disponible" to (nuevoDisponible > 0)
            )

            inventarioRepository.updateInventarioFields(id, updates).onSuccess {
                _uiState.update { it.copy(isLoading = false, error = null) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun cargarItemInventarioEspecifico(id: String) {
        if (id.isEmpty()) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            inventarioRepository.observeInventarioItem(id).collect { item ->
                item?.let { newItem ->
                    _uiState.update { state ->
                        val nuevaLista = state.inventories.toMutableList()
                        val index = nuevaLista.indexOfFirst { it.id == id }
                        if (index != -1) nuevaLista[index] = newItem else nuevaLista.add(newItem)
                        state.copy(inventories = nuevaLista, isLoading = false)
                    }
                    cargarNombreLibro(newItem.libro_id)
                }
            }
        }
    }

    fun eliminarItemInventario(id: String, onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            inventarioRepository.deleteInventario(id).onSuccess {
                _uiState.update { state ->
                    val nuevaLista = state.inventories.filter { it.id != id }
                    state.copy(inventories = nuevaLista, isLoading = false)
                }
                onSuccess()
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = "error_delete_prefix") }
            }
        }
    }

    fun cargarNombreLibro(libroId: String) {
        if (libroId.isEmpty() || _libroNombres.value.containsKey(libroId)) return
        viewModelScope.launch {
            val names = libroRepository.getLibroNames(listOf(libroId))
            _libroNombres.update { it + names }
        }
    }

    fun cargarNombresMasivos(libroIds: List<String>) {
        val idsToLoad = libroIds.filter { it.isNotEmpty() && !_libroNombres.value.containsKey(it) }.distinct()
        if (idsToLoad.isEmpty()) return
        viewModelScope.launch {
            val names = libroRepository.getLibroNames(idsToLoad)
            _libroNombres.update { it + names }
        }
    }

    fun cargarNombreBiblioteca(bibliotecaId: String) {
        if (bibliotecaId.isEmpty()) {
            _nombreBibliotecaUsuario.value = "unassigned_location"
            return
        }
        viewModelScope.launch {
            val bib = bibliotecaRepository.getBiblioteca(bibliotecaId)
            _nombreBibliotecaUsuario.update { bib?.nombre ?: "no_branch_name" }
        }
    }

    fun getNombreCategoria(categoriaId: String): String {
        return _uiState.value.categorias.find { it.categoriaId == categoriaId }?.nombre ?: "no_category_found"
    }

    fun solicitarPrestamo(
        libroId: String,
        isbn: String,
        bibliotecaId: String,
        fechaDevolucion: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        val nuevoPrestamo = Prestamo(
            prestamoid = UUID.randomUUID().toString(),
            usuario_id = userId,
            libro_id = libroId,
            isbn = isbn,
            biblioteca_id = bibliotecaId,
            fecha_devolucion = fechaDevolucion,
            estado = "pendiente"
        )
        viewModelScope.launch {
            prestamoRepository.crearPrestamo(nuevoPrestamo).onSuccess {
                onSuccess()
            }.onFailure { e ->
                onError(e.message ?: "Error desconocido")
            }
        }
    }
}