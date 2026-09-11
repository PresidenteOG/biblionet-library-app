package com.biblionet.ui.viewmodel.book

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biblionet.data.model.book.Libro
import com.biblionet.ui.view.book.MisLibrosTab
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Locale

data class PrestamoConLibro(
    val libro: Libro,
    val fecha: String,
    val estado: String
)

class MisLibrosViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var prestamosAMostrar = mutableStateListOf<PrestamoConLibro>()
        private set

    private var listaCompletaCache = mutableListOf<PrestamoConLibro>()
    var isLoading by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var isRealTimeSearch by mutableStateOf(true)


    fun cargarDatosSegunTab(tab: MisLibrosTab) {
        val uid = auth.currentUser?.uid ?: return
        isLoading = true

        val coleccion = if (tab == MisLibrosTab.FAVORITOS) "favoritos" else "prestamos"

        db.collection(coleccion)
            .whereEqualTo("usuario_id", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                viewModelScope.launch {
                    val listaCargada = mutableListOf<PrestamoConLibro>()
                    val docs = snapshot?.documents ?: emptyList()

                    // 1. Obtenemos el tiempo actual en milisegundos
                    val hoy = System.currentTimeMillis()

                    // 2. Definimos la duración de un mes (30 días) en milisegundos
                    // (30 días * 24 horas * 60 min * 60 seg * 1000 ms)
                    val unMesEnMillis = 30L * 24 * 60 * 60 * 1000

                    for (doc in docs) {
                        val libroId = doc.getString("libro_id") ?: ""
                        val estadoOriginal = doc.getString("estado") ?: ""
                        val milisegundosSalida = doc.getLong("fecha_salida") ?: 0L

                        // 3. Lógica de "Retrasado" automática
                        // Si el libro está "activo" y (fecha_salida + 30 días) es menor que hoy -> Retrasado
                        val estadoDoc = if (estadoOriginal == "reservado" && (milisegundosSalida + unMesEnMillis) < hoy) {
                            "retrasado"
                        } else {
                            // Si no está retrasado, mantenemos el estado original de Firebase
                            if (estadoOriginal.isEmpty() && tab == MisLibrosTab.FAVORITOS) "" else estadoOriginal
                        }

                        // 4. Formateo de la fecha para la UI
                        val fechaStr = if (milisegundosSalida > 0) {
                            val date = java.util.Date(milisegundosSalida)
                            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(date)
                        } else {
                            ""
                        }

                        if (libroId.isNotEmpty()) {
                            val libroDoc = db.collection("libros").document(libroId).get().await()
                            libroDoc.toObject(Libro::class.java)?.let { libro ->
                                // Guardamos el ID del documento de PRESTAMO para poder diferenciarlos
                                listaCargada.add(PrestamoConLibro(libro, fechaStr, estadoDoc))
                            }
                        }
                    }

                    // --- LÓGICA DE FILTRADO Y REPETIDOS ---
                    listaCompletaCache = when(tab) {
                        MisLibrosTab.RESERVAS -> {
                            listaCargada.filter {
                                it.estado != "devuelto" && it.estado != "rechazado"
                            }.toMutableList()
                            // ^ En RESERVAS sí hacemos distinctBy porque no puedes tener
                            // el mismo libro reservado dos veces a la vez.
                        }
                        MisLibrosTab.HISTORIAL -> {
                            listaCargada.filter { it.estado == "devuelto" }
                                .sortedByDescending { it.fecha } // Los más recientes primero
                                .toMutableList()
                            // ^ En HISTORIAL NO HACEMOS distinctBy.
                            // Si lo leyó 3 veces, salen 3 líneas. ¡Es un historial!
                        }
                        else -> {
                            listaCargada.distinctBy { it.libro.libro_id }.toMutableList()
                        }
                    }

                    actualizarFiltro()
                    isLoading = false
                }
            }
    }

    fun ejecutarBusquedaManual() {
        actualizarFiltro() // Esta función ya limpia y filtra prestamosAMostrar
    }

    // Mantenemos tus funciones de búsqueda originales
    fun onSearchQueryChange(nuevoTexto: String) {
        searchQuery = nuevoTexto
        if (isRealTimeSearch) actualizarFiltro()
    }

    fun onToggleSearchMode() { isRealTimeSearch = !isRealTimeSearch }

    private fun actualizarFiltro() {
        prestamosAMostrar.clear()
        if (searchQuery.isEmpty()) {
            prestamosAMostrar.addAll(listaCompletaCache)
        } else {
            prestamosAMostrar.addAll(
                listaCompletaCache.filter {
                    it.libro.titulo.contains(searchQuery, ignoreCase = true) ||
                            it.libro.autor.contains(searchQuery, ignoreCase = true)
                }
            )
        }
    }
}