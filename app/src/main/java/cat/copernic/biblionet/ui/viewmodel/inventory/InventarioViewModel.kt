// Archivo: cat/copernic/biblionet/ui/viewmodel/inventory/InventarioViewModel.kt
package cat.copernic.biblionet.ui.viewmodel.inventory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.copernic.biblionet.data.model.inventory.Inventario // IMPORT AÑADIDO
import cat.copernic.biblionet.data.repository.inventory.InventarioRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class InventarioViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val repository = InventarioRepository(db)

    var inventario by mutableStateOf<List<Inventario>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var isSuccess by mutableStateOf(false)
        private set

    init {
        observeInventario()
    }

    private fun observeInventario() {
        isLoading = true
        viewModelScope.launch {
            repository.observeInventario()
                .catch { e ->
                    error = e.message
                    isLoading = false
                }
                .collect { items ->
                    inventario = items
                    isLoading = false
                }
        }
    }

    fun crearInventario(inventario: Inventario) {
        viewModelScope.launch {
            isLoading = true
            error = null
            repository.createInventario(inventario)
                .onSuccess {
                    isSuccess = true
                    isLoading = false
                }
                .onFailure { e ->
                    error = e.message
                    isLoading = false
                }
        }
    }

    fun actualizarInventario(inventario: Inventario) {
        viewModelScope.launch {
            isLoading = true
            repository.updateInventario(inventario)
                .onSuccess { isLoading = false }
                .onFailure { e ->
                    error = e.message
                    isLoading = false
                }
        }
    }

    fun actualizarStock(inventarioId: String, cantidad: Int) {
        val item = inventario.find { it.id == inventarioId }
        item?.let {
            it.actualizarStock(cantidad)
            actualizarInventario(it)
        }
    }

    fun eliminarInventario(inventario: Inventario) {
        viewModelScope.launch {
            isLoading = true
            repository.deleteInventario(inventario.id)
                .onSuccess { isLoading = false }
                .onFailure { e ->
                    error = e.message
                    isLoading = false
                }
        }
    }

    fun resetStates() {
        isSuccess = false
        error = null
    }

    override fun onCleared() {
        super.onCleared()
    }
}