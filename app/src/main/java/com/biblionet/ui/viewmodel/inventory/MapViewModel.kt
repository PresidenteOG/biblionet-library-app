// Archivo: src/main/java/cat/copernic/biblionet/ui/viewmodel/inventory/MapViewModel.kt
package com.biblionet.ui.viewmodel.inventory

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biblionet.R
import com.biblionet.data.model.library.Biblioteca
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

// MODELO LIGERO EXCLUSIVO PARA EL MAPA (Une los datos del Libro + su Disponibilidad)
data class MapLibro(
    val id: String,
    val titulo: String,
    val autor: String,
    val isbn: String,
    val disponible: Boolean,
    val portadaUrl: String
)

class MapViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private var searchJob: Job? = null

    var todasLasBibliotecas = listOf<Biblioteca>()
        private set
    var filteredBibliotecas by mutableStateOf<List<Biblioteca>>(emptyList())
        private set
    var closestBibliotecas by mutableStateOf<List<Biblioteca>>(emptyList())
        private set
    var selectedBiblioteca by mutableStateOf<Biblioteca?>(null)
        private set
    var librosDeBiblioteca by mutableStateOf<List<MapLibro>>(emptyList())
        private set
    var selectedLibro by mutableStateOf<MapLibro?>(null)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var addressSuggestions by mutableStateOf<List<Address>>(emptyList())
        private set
    var isFilterVisible by mutableStateOf(false)
        private set
    var maxDistanceInput by mutableStateOf("")
        private set
    var userLocation by mutableStateOf(GeoPoint(41.3851, 2.1734))
        private set
    var searchedLocationMarker by mutableStateOf<GeoPoint?>(null)
        private set
    var isLoadingMap by mutableStateOf(true)
        private set
    var isLoadingBooks by mutableStateOf(false)
        private set

    init { cargarBibliotecas() }

    private fun cargarBibliotecas() {
        isLoadingMap = true
        db.collection("bibliotecas").get()
            .addOnSuccessListener { snapshot ->
                todasLasBibliotecas = snapshot.documents.mapNotNull { doc ->
                    try {
                        Biblioteca(
                            id = doc.id,
                            nombre = doc.getString("nombre") ?: "Biblioteca sin nombre",
                            direccion = doc.getString("direccion") ?: "",
                            telefono = doc.getString("telefono") ?: "",
                            accesibilidad = doc.getBoolean("accesibilidad") ?: false,
                            email = doc.getString("email") ?: "",
                            descripcion = doc.getString("descripcion") ?: "",
                            fotoUrl = doc.getString("foto_url") ?: doc.getString("fotoUrl") ?: "",
                            latitud = doc.getDouble("latitud") ?: 0.0,
                            longitud = doc.getDouble("longitud") ?: 0.0,
                            horario = (doc.get("horario") as? Map<String, List<String>>) ?: emptyMap()
                        )
                    } catch (e: Exception) { null }
                }
                aplicarFiltros()
                isLoadingMap = false
            }.addOnFailureListener { isLoadingMap = false }
    }

    private fun getLocalizedDayName(context: Context, dbDay: String): String {
        return when(dbDay) {
            "Lunes" -> context.getString(R.string.dia_lunes)
            "Martes" -> context.getString(R.string.dia_martes)
            "Miércoles" -> context.getString(R.string.dia_miercoles)
            "Jueves" -> context.getString(R.string.dia_jueves)
            "Viernes" -> context.getString(R.string.dia_viernes)
            "Sábado" -> context.getString(R.string.dia_sabado)
            "Domingo" -> context.getString(R.string.dia_domingo)
            "Festivos" -> context.getString(R.string.dia_festivos)
            else -> dbDay
        }
    }

    fun obtenerEstadoApertura(biblioteca: Biblioteca, context: Context): Pair<String, Boolean> {
        if (biblioteca.horario.isEmpty()) return Pair(context.getString(R.string.horario_no_disponible), false)

        val calendar = Calendar.getInstance()
        val diasSemanaArray = arrayOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
        val currentDayIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val diaSemana = diasSemanaArray[currentDayIndex]

        val horasDia = biblioteca.horario[diaSemana]
        val franjasActivas = horasDia?.filter { it.isNotBlank() && !it.equals("cerrado", ignoreCase = true) } ?: emptyList()
        val ahora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        if (franjasActivas.isNotEmpty()) {
            for (franja in franjasActivas) {
                val partes = franja.split("-")
                if (partes.size == 2) {
                    val apertura = partes[0].trim()
                    val cierre = partes[1].trim()
                    if (ahora in apertura..cierre) {
                        return Pair(context.getString(R.string.abierto_hasta, cierre), true)
                    } else if (ahora < apertura) {
                        return Pair(context.getString(R.string.cerrado_abre_hoy, apertura), false)
                    }
                }
            }
        }

        for (i in 1..7) {
            val nextDayIndex = (currentDayIndex + i) % 7
            val nextDayName = diasSemanaArray[nextDayIndex]
            val nextDayHours = biblioteca.horario[nextDayName]?.filter { it.isNotBlank() && !it.equals("cerrado", ignoreCase = true) } ?: emptyList()

            if (nextDayHours.isNotEmpty()) {
                val firstOpen = nextDayHours[0].split("-")[0].trim()
                if (i == 1) {
                    return Pair(context.getString(R.string.cerrado_abre_manana, firstOpen), false)
                } else {
                    val diaTraducido = getLocalizedDayName(context, nextDayName)
                    return Pair(context.getString(R.string.cerrado_abre_dia, diaTraducido, firstOpen), false)
                }
            }
        }

        return Pair(context.getString(R.string.cerrado_temporalmente), false)
    }

    fun onSearchQueryChanged(query: String, context: Context) {
        searchQuery = query
        if (query.isBlank()) searchedLocationMarker = null
        aplicarFiltros()

        searchJob?.cancel()
        if (query.length > 3) {
            searchJob = viewModelScope.launch { delay(600); obtenerSugerencias(context, query) }
        } else {
            addressSuggestions = emptyList()
        }
    }

    private suspend fun obtenerSugerencias(context: Context, query: String) {
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName(query, 5) { addresses -> addressSuggestions = addresses }
                } else {
                    @Suppress("DEPRECATION")
                    val results = geocoder.getFromLocationName(query, 5)
                    if (results != null) addressSuggestions = results
                }
            } catch (e: Exception) { addressSuggestions = emptyList() }
        }
    }

    fun selectSuggestion(address: Address) {
        val newPoint = GeoPoint(address.latitude, address.longitude)
        searchedLocationMarker = newPoint
        searchQuery = address.getAddressLine(0) ?: searchQuery
        addressSuggestions = emptyList()
        aplicarFiltros()
    }

    fun toggleFilterVisibility() { isFilterVisible = !isFilterVisible }

    fun onMaxDistanceInputChanged(newInput: String) {
        if (newInput.isEmpty() || newInput.all { it.isDigit() }) {
            maxDistanceInput = newInput
            aplicarFiltros()
        }
    }

    fun updateUserLocation(location: GeoPoint) {
        userLocation = location
        searchedLocationMarker = null
        aplicarFiltros()
    }

    private fun aplicarFiltros() {
        val distanceLimit = if (maxDistanceInput.isEmpty()) 99999.0 else maxDistanceInput.toDoubleOrNull() ?: 50.0
        var temporalList = todasLasBibliotecas

        if (searchQuery.isNotBlank() && addressSuggestions.isEmpty() && searchedLocationMarker == null) {
            temporalList = temporalList.filter {
                it.nombre.contains(searchQuery, ignoreCase = true) ||
                        it.direccion.contains(searchQuery, ignoreCase = true)
            }
        }

        temporalList = temporalList.filter { biblio ->
            val biblioPoint = GeoPoint(biblio.latitud, biblio.longitud)
            val originPoint = searchedLocationMarker ?: userLocation
            val distanceInKm = originPoint.distanceToAsDouble(biblioPoint) / 1000.0
            distanceInKm <= distanceLimit
        }
        filteredBibliotecas = temporalList

        val originForClosest = searchedLocationMarker ?: userLocation
        closestBibliotecas = todasLasBibliotecas.sortedBy { biblio ->
            val biblioPoint = GeoPoint(biblio.latitud, biblio.longitud)
            originForClosest.distanceToAsDouble(biblioPoint)
        }.take(3)
    }

    // NUEVO: Función para calcular los km exactos a mostrar en la tarjeta
    fun calcularDistancia(biblioteca: Biblioteca): Double {
        val originPoint = searchedLocationMarker ?: userLocation
        val biblioPoint = GeoPoint(biblioteca.latitud, biblioteca.longitud)
        return originPoint.distanceToAsDouble(biblioPoint) / 1000.0
    }

    fun onBibliotecaSelected(biblioteca: Biblioteca?) {
        selectedBiblioteca = biblioteca
        if (biblioteca != null) cargarLibrosDefinitivo(biblioteca.id)
    }

    private fun cargarLibrosDefinitivo(bibliotecaId: String) {
        isLoadingBooks = true
        val targetId = bibliotecaId.trim()

        db.collection("inventario").get().addOnSuccessListener { invSnapshot ->
            val stockMap = invSnapshot.documents.associate {
                val lid = it.getString("libro_id") ?: it.getString("libroId") ?: ""
                val bid = it.getString("biblioteca_id") ?: it.getString("bibliotecaId") ?: ""
                val estaDisponible = it.getBoolean("disponible") ?: true
                val stockReal = it.getLong("stock_disponible") ?: 1L
                val hayStock = estaDisponible && (stockReal > 0L)
                (lid.trim() + bid.trim()) to hayStock
            }

            db.collection("libros").get().addOnSuccessListener { libSnapshot ->
                val resultado = mutableListOf<MapLibro>()
                for (doc in libSnapshot.documents) {
                    val bibIdEnLibro = doc.getString("bibliotecaId")?.trim() ?: doc.getString("biblioteca_id")?.trim() ?: ""
                    val idDoc = doc.id.trim()
                    val libroIdField = doc.getString("libro_id")?.trim() ?: ""

                    val idEnInventario = stockMap.containsKey(idDoc + targetId)
                    val idFieldEnInventario = if (libroIdField.isNotEmpty()) stockMap.containsKey(libroIdField + targetId) else false

                    if (bibIdEnLibro == targetId || idEnInventario || idFieldEnInventario) {
                        val esDisponible = stockMap[idDoc + targetId] ?: stockMap[libroIdField + targetId] ?: doc.getBoolean("disponible") ?: true
                        resultado.add(
                            MapLibro(
                                id = idDoc,
                                titulo = doc.getString("titulo") ?: "Sin título",
                                autor = doc.getString("autor") ?: "Desconocido",
                                isbn = doc.getString("isbn") ?: idDoc,
                                disponible = esDisponible,
                                portadaUrl = doc.getString("imagen_url") ?: ""
                            )
                        )
                    }
                }
                librosDeBiblioteca = resultado
                isLoadingBooks = false
            }.addOnFailureListener { librosDeBiblioteca = emptyList(); isLoadingBooks = false }
        }.addOnFailureListener { isLoadingBooks = false }
    }

    fun onLibroSelected(libro: MapLibro?) { selectedLibro = libro }
    fun clearSelection() { selectedBiblioteca = null; selectedLibro = null }
}