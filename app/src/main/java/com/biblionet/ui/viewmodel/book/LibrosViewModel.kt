package com.biblionet.ui.viewmodel.book

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biblionet.data.model.book.Libro
import com.biblionet.data.model.book.Categoria
import com.biblionet.data.repository.book.CategoriaRepository
import com.biblionet.data.repository.book.LibroRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import com.biblionet.R

/**
 * ViewModel encargado de la gestión y visualización de la lista de libros.
 * Permite filtrar libros por categoría y realizar operaciones de borrado.
 */
class LibrosViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val libroRepository = LibroRepository(db)
    private val categoriaRepository = CategoriaRepository()

    private var _listaCompletaLibros = listOf<Libro>()
    
    /**
     * Lista de libros filtrada para mostrar en la interfaz.
     */
    var libros by mutableStateOf<List<Libro>>(emptyList())
        private set

    /**
     * Lista de todas las categorías disponibles.
     */
    var categorias by mutableStateOf<List<Categoria>>(emptyList())
        private set

    /**
     * Estado que indica si la carga de datos está en curso.
     */
    var isLoading by mutableStateOf(true)
        private set

    /**
     * ID de la categoría actualmente seleccionada para el filtrado. "Todos" por defecto.
     */
    var selectedCategoriaId by mutableStateOf("Todos")
        private set

    /**
     * Mensaje de error general.
     */
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var listener: ListenerRegistration? = null

    init {
        observeLibros()
        obtenerCategorias()
    }

    /**
     * Establece un listener en tiempo real para la colección de libros.
     */
    private fun observeLibros() {
        listener = libroRepository.observeLibros(
            onSuccess = { lista ->
                _listaCompletaLibros = lista
                actualizarFiltro()
                isLoading = false
            },
            onError = { error ->
                errorMessage = error
                isLoading = false
            }
        )
    }

    /**
     * Recupera las categorías desde el repositorio.
     */
    private fun obtenerCategorias() {
        viewModelScope.launch {
            categoriaRepository.getCategorias().collect { lista ->
                categorias = lista
            }
        }
    }

    /**
     * Actualiza la categoría seleccionada y aplica el filtro a la lista de libros.
     * @param categoriaId ID de la categoría a filtrar.
     */
    fun onCategoriaSelected(categoriaId: String) {
        selectedCategoriaId = categoriaId
        actualizarFiltro()
    }

    /**
     * Filtra la lista completa de libros basándose en [selectedCategoriaId].
     */
    private fun actualizarFiltro() {
        libros = if (selectedCategoriaId == "Todos") {
            _listaCompletaLibros
        } else {
            _listaCompletaLibros.filter { it.categoria_id == selectedCategoriaId }
        }
    }

    /**
     * ID de recurso de cadena para mostrar mensajes de error localizados.
     */
    var errorResId by mutableStateOf<Int?>(null)
        private set

    /**
     * Elimina un libro del sistema.
     * Realiza validaciones previas para asegurar que no tenga inventario o préstamos asociados.
     * @param libroId ID del libro a eliminar.
     */
    fun eliminarLibro(libroId: String) {
        viewModelScope.launch {
            libroRepository.deleteLibro(libroId)
                .onSuccess {
                    errorResId = null // Todo bien
                }
                .onFailure { e ->
                    // Mapeamos el código del repo al string correspondiente
                    errorResId = when (e.message) {
                        "ERR_HAS_INVENTORY" -> R.string.error_inventario_asociado
                        "ERR_HAS_LOANS" -> R.string.error_prestamos_asociados
                        "ERR_HAS_HISTORY" -> R.string.confirm_delete_history
                        else -> R.string.error_delete_libro
                    }
                }
        }
    }

    /**
     * Limpia los estados de error actuales.
     */
    fun clearError() {
        errorMessage = null
        errorResId = null
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}
