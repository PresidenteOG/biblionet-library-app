// Archivo: cat/copernic/biblionet/ui/viewmodel/transaction/PrestamoViewModel.kt
package com.biblionet.ui.viewmodel.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biblionet.data.model.transaction.Prestamo
import com.biblionet.data.repository.transaction.PrestamoRepository
import com.biblionet.utils.GlobalState
import com.biblionet.utils.PenalizacionConfig
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PrestamoViewModel : ViewModel() {
    private val repository = PrestamoRepository(FirebaseFirestore.getInstance())

    var prestamosPendientes by mutableStateOf<List<Prestamo>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        observePendientes()
    }

    private fun observePendientes() {
        val bibliotecaId = GlobalState.usuario?.biblioteca_id ?: return
        isLoading = true
        repository.observePrestamosByBiblioteca(bibliotecaId) { list ->
            // 1. Verificamos si hay libros que acaban de pasar a estar retrasados
            verificarRetrasos(list)

            // 2. Actualizamos la UI
            prestamosPendientes = list
            isLoading = false
        }
    }

    fun aceptarReserva(prestamo: Prestamo) {
        viewModelScope.launch {
            isLoading = true
            val result = repository.procesarAceptar(prestamo)
            if (result.isFailure) {
                error = result.exceptionOrNull()?.message
            }
            isLoading = false
        }
    }

    fun rechazarReserva(prestamoId: String) {
        viewModelScope.launch {
            isLoading = true
            val result = repository.rechazarReserva(prestamoId)
            if (result.isFailure) {
                error = result.exceptionOrNull()?.message
            }
            isLoading = false
        }
    }

    fun devolverLibro(prestamo: Prestamo) {
        viewModelScope.launch {
            isLoading = true
            repository.procesarDevolucion(prestamo)
                .onSuccess {
                    // ¡AQUÍ SUMAMOS EL LIBRO LEÍDO AL PERFIL DEL USUARIO!
                    FirebaseFirestore.getInstance()
                        .collection("usuarios")
                        .document(prestamo.usuario_id)
                        .update("libros_leidos", FieldValue.increment(1))

                    isLoading = false
                }
                .onFailure { error ->
                    this@PrestamoViewModel.error = error.message ?: "Error desconocido"
                    isLoading = false
                }
        }
    }

    private fun verificarRetrasos(lista: List<Prestamo>) {
        val ahora = System.currentTimeMillis()

        lista.forEach { prestamo ->
            // Si el libro está prestado (activo) pero la fecha de devolución ya pasó
            if (prestamo.estado == "activo" && prestamo.fecha_devolucion < ahora) {
                actualizarEstadoARetrasado(prestamo)
            }
        }
    }

    //Solo cambia el estado del préstamo
    private fun actualizarEstadoARetrasado(prestamo: Prestamo) {
        viewModelScope.launch {
            // Actualizamos en Firebase para que el Badge cambie a ROJO automáticamente
            FirebaseFirestore.getInstance()
                .collection("prestamos")
                .document(prestamo.prestamoid)
                .update("estado", "retrasado")
                .await()

            bloquearUsuarioPorDias(prestamo.usuario_id,PenalizacionConfig.DIAS_POR_RETRASO)
        }
    }

    //  2: Solo aplica el bloqueo al usuario
    suspend fun bloquearUsuarioPorDias(usuarioId: String, dias: Int): Result<Unit> = try {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, dias)
        val fechaBloqueo = Timestamp(calendar.time)

        FirebaseFirestore.getInstance().collection("usuarios").document(usuarioId)
            .update("bloqueado_hasta", fechaBloqueo)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    fun clearError() {
        error = null
    }
}