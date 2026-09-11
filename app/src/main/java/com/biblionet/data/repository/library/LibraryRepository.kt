// Archivo: cat/copernic/biblionet/data/repository/library/LibraryRepository.kt
package com.biblionet.data.repository.library

// CORRECCIÓN CLAVE: Importamos el modelo desde 'library', no desde 'inventory'
import com.biblionet.data.model.library.Biblioteca
import com.biblionet.data.model.inventory.LibroCartMapView
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.UUID

// ==========================================
// 1. EL REPOSITORIO PRINCIPAL
// ==========================================
class LibraryRepository {
    private val api = RetrofitClient.instance

    suspend fun fetchBibliotecasMunicipales(): List<Biblioteca> {
        return try {
            val response = api.getBibliotecas()
            val apiLibs = response.elements.mapNotNull { element ->
                try {
                    val lon = element.localitzacio_x?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                    val lat = element.localitzacio_y?.replace(",", ".")?.toDoubleOrNull() ?: 0.0

                    if (lat != 0.0 && lon != 0.0) {
                        Biblioteca(
                            id = element.nom ?: UUID.randomUUID().toString(),
                            nombre = element.nom ?: "Biblioteca Desconocida",
                            latitud = lat,
                            longitud = lon,
                            direccion = element.adreca_nom ?: "Dirección no disponible",
                            accesibilidad = true // Por defecto las de la DIBA asumimos que son accesibles
                        )
                    } else null
                } catch (e: Exception) { null }
            }

            // Si la API funcionó y devolvió bibliotecas, las usamos.
            // Si la DIBA devuelve 0 bibliotecas, usamos el PLAN B.
            if (apiLibs.isNotEmpty()) apiLibs else getBibliotecasPlanB()

        } catch (e: Exception) {
            e.printStackTrace()
            // Si no hay internet o la API de la DIBA da error 404, usamos el PLAN B.
            getBibliotecasPlanB()
        }
    }

    // --- EL PLAN B (DATOS REALES GARANTIZADOS CON ARGUMENTOS NOMBRADOS) ---
    private fun getBibliotecasPlanB(): List<Biblioteca> {
        return listOf(
            Biblioteca(id = "1", nombre = "Biblioteca Sagrada Família", latitud = 41.4042, longitud = 2.1751, direccion = "Carrer de Provença, 480 (Barcelona)", accesibilidad = true),
            Biblioteca(id = "2", nombre = "Biblioteca Ignasi Iglésias", latitud = 41.4326, longitud = 2.1834, direccion = "Carrer de la Selva de Mar, 215 (Barcelona)", accesibilidad = true),
            Biblioteca(id = "3", nombre = "Biblioteca Vapor Badia", latitud = 41.5456, longitud = 2.1118, direccion = "Carrer de Pi i Margall, 115 (Sabadell)", accesibilidad = true),
            Biblioteca(id = "4", nombre = "Biblioteca Central de Terrassa", latitud = 41.5654, longitud = 2.0123, direccion = "Passeig de les Lletres, 1 (Terrassa)", accesibilidad = true),
            Biblioteca(id = "5", nombre = "Biblioteca Pompeu Fabra", latitud = 41.5381, longitud = 2.4447, direccion = "Plaça de l'Escorxador, s/n (Mataró)", accesibilidad = true),
            Biblioteca(id = "6", nombre = "Biblioteca Marc de Vilalba", latitud = 41.6401, longitud = 2.3582, direccion = "Carrer Major, 14 (Cardedeu)", accesibilidad = true),
            Biblioteca(id = "7", nombre = "Biblioteca Ateneu Les Bases", latitud = 41.7335, longitud = 1.8262, direccion = "Carrer d'Àngel Guimerà, 73 (Manresa)", accesibilidad = true),
            Biblioteca(id = "8", nombre = "Biblioteca Torras i Bages", latitud = 41.3468, longitud = 1.7013, direccion = "Carrer de la Creu, 32 (Vilafranca)", accesibilidad = true)
        )
    }

    suspend fun fetchLibrosDeBiblioteca(bibliotecaId: String): List<LibroCartMapView> {
        delay(800)
        // Usamos los nombres explícitos para evitar errores futuros
        return listOf(
            LibroCartMapView(isbn = "123", titulo = "El Quijote", autor = "Miguel de Cervantes", portadaUrl = "https://covers.openlibrary.org/b/id/8259441-M.jpg", disponible = true),
            LibroCartMapView(isbn = "456", titulo = "1984", autor = "George Orwell", portadaUrl = "https://covers.openlibrary.org/b/id/153256-M.jpg", disponible = false),
            LibroCartMapView(isbn = "789", titulo = "La Sombra del Viento", autor = "Carlos Ruiz Zafón", portadaUrl = "https://covers.openlibrary.org/b/id/10521270-M.jpg", disponible = true)
        )
    }
}

// ==========================================
// 2. CONFIGURACIÓN DE RETROFIT (API DIBA)
// ==========================================

// Modelos exactos para leer el JSON de la DIBA
data class DibaResponse(val elements: List<DibaElement>)
data class DibaElement(
    val adreca_nom: String?,
    val nom: String?,
    val localitzacio_x: String?, // Longitud
    val localitzacio_y: String?  // Latitud
)

// Interfaz de Retrofit
interface DibaApiService {
    @GET("api/dataset/201/format/json")
    suspend fun getBibliotecas(): DibaResponse
}

// Creador del cliente de internet (Singleton)
object RetrofitClient {
    val instance: DibaApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://dadesobertes.diba.cat/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DibaApiService::class.java)
    }
}