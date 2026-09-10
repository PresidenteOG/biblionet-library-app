package cat.copernic.biblionet.ui.viewmodel.inicio

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import cat.copernic.biblionet.data.model.book.Libro
import cat.copernic.biblionet.data.model.book.Categoria
// Daniel's Corrected Import
import cat.copernic.biblionet.data.repository.book.CategoriaRepository
import kotlinx.coroutines.launch

enum class SortOrder {
    NONE, ASCENDING, DESCENDING, NEWEST, OLDEST
}

class InicioViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val categoriaRepository = CategoriaRepository()

    private var listaCompletaLibros = mutableListOf<Libro>()

    var libros = mutableStateListOf<Libro>()
        private set

    var categorias = mutableStateListOf<Categoria>()
        private set

    var searchQuery by mutableStateOf("")
        private set

    var selectedCategoriaId by mutableStateOf("Todos")
        private set

    var sortOrder by mutableStateOf(SortOrder.NONE)
        private set

    var selectedRating by mutableStateOf(0)
        private set

    init {
        obtenerLibros()
        obtenerCategorias()
    }

    private fun obtenerLibros() {
        db.collection("libros").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                listaCompletaLibros = snapshot.toObjects(Libro::class.java).toMutableList()
                actualizarFiltro()
            }
        }
    }

    private fun obtenerCategorias() {
        viewModelScope.launch {
            categoriaRepository.getCategorias().collect { lista ->
                categorias.clear()
                categorias.addAll(lista)
            }
        }
    }

    fun onSearchQueryChange(nuevoTexto: String) {
        searchQuery = nuevoTexto
        actualizarFiltro()
    }

    fun onCategoriaSelected(categoriaId: String) {
        selectedCategoriaId = categoriaId
        actualizarFiltro()
    }

    fun onSortOrderSelected(order: SortOrder) {
        sortOrder = order
        actualizarFiltro()
    }

    fun onRatingSelected(rating: Int) {
        selectedRating = rating
        actualizarFiltro()
    }

    private fun actualizarFiltro() {
        libros.clear()

        var filtrados = if (searchQuery.isEmpty()) {
            listaCompletaLibros
        } else {
            listaCompletaLibros.filter {
                it.titulo.contains(searchQuery, ignoreCase = true) ||
                        it.autor.contains(searchQuery, ignoreCase = true)
            }
        }

        if (selectedCategoriaId != "Todos") {
            filtrados = filtrados.filter { it.categoria_id == selectedCategoriaId }
        }

        // Optimized Filter: Using the Double 'estrellas' field from our merged Libro model
        if (selectedRating > 0) {
            filtrados = filtrados.filter { it.estrellas >= selectedRating.toDouble() }
        }

        val sortedList = when (sortOrder) {
            SortOrder.ASCENDING -> filtrados.sortedBy { it.estrellas }
            SortOrder.DESCENDING -> filtrados.sortedByDescending { it.estrellas }
            SortOrder.NEWEST -> filtrados.sortedByDescending { it.fecha_creacion?.seconds ?: 0L }
            SortOrder.OLDEST -> filtrados.sortedBy { it.fecha_creacion?.seconds ?: Long.MAX_VALUE }
            SortOrder.NONE -> filtrados
        }

        libros.addAll(sortedList)
    }
}