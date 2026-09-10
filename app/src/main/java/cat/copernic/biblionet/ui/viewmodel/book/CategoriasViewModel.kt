// Archivo: cat/copernic/biblionet/ui/viewmodel/book/CategoriasViewModel.kt
package cat.copernic.biblionet.ui.viewmodel.book

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.copernic.biblionet.data.model.book.Categoria
// IMPORT CORREGIDO:
import cat.copernic.biblionet.data.repository.book.CategoriaRepository
import cat.copernic.biblionet.data.remote.cloudinary.CloudinaryRepository
import kotlinx.coroutines.launch

class CategoriasViewModel(
    private val repository: CategoriaRepository = CategoriaRepository(),
    private val cloudinaryRepository: CloudinaryRepository = CloudinaryRepository()
) : ViewModel() {

    var categorias by mutableStateOf<List<Categoria>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isUploading by mutableStateOf(false)
        private set

    // Para mostrar mensajes en la UI
    var mensajeFeedback by mutableStateOf<String?>(null)
        private set

    init {
        observarCategorias()
    }

    private fun observarCategorias() {
        viewModelScope.launch {
            repository.getCategorias().collect { lista ->
                categorias = lista
                Log.d("FIRESTORE_DEBUG", "Categorías cargadas: ${lista.size}")
            }
        }
    }

    fun uploadImage(context: Context, uri: Uri, onResult: (String) -> Unit) {
        isUploading = true
        cloudinaryRepository.uploadProfileImage(context, uri) { url ->
            isUploading = false
            if (url != null) {
                onResult(url)
            } else {
                mensajeFeedback = "Error al subir la imagen"
            }
        }
    }

    fun guardarCategoria(categoria: Categoria) {
        viewModelScope.launch {
            try {
                isLoading = true
                Log.d("FIRESTORE_DEBUG", "Intentando guardar: $categoria")

                repository.saveCategoria(categoria)

                Log.d("FIRESTORE_DEBUG", "¡Guardado exitoso en Firestore!")
                mensajeFeedback = "Categoría guardada correctamente"
            } catch (e: Exception) {
                Log.e("FIRESTORE_DEBUG", "Error al guardar categoría: ${e.message}")
                e.printStackTrace()
                mensajeFeedback = "Error al guardar: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun borrarMensaje() { mensajeFeedback = null }

    fun eliminarCategoria(categoriaId: String) {
        viewModelScope.launch {
            try {
                isLoading = true
                repository.deleteCategoria(categoriaId)
                mensajeFeedback = "Categoría eliminada"
            } catch (e: Exception) {
                Log.e("FIRESTORE_DEBUG", "Error al eliminar: ${e.message}")
                mensajeFeedback = "Error al eliminar"
            } finally {
                isLoading = false
            }
        }
    }
}