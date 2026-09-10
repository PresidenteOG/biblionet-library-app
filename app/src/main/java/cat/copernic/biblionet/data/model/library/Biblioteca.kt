// Archivo: cat/copernic/biblionet/data/model/library/Biblioteca.kt
package cat.copernic.biblionet.data.model.library

import com.google.firebase.firestore.PropertyName

/**
 * Representa una biblioteca dentro del sistema BiblioNet.
 *
 * Contiene la información básica de contacto, ubicación geográfica
 * y horarios de apertura de la biblioteca almacenada en Firestore.
 *
 * @property id Identificador único de la biblioteca.
 * @property nombre Nombre de la biblioteca.
 * @property direccion Dirección física de la biblioteca.
 * @property telefono Número de teléfono de contacto.
 * @property accesibilidad Indica si la biblioteca cuenta con accesibilidad para personas con movilidad reducida.
 * @property email Correo electrónico de contacto de la biblioteca.
 * @property descripcion Descripción general de la biblioteca o sus servicios.
 * @property fotoUrl URL de la imagen representativa de la biblioteca almacenada en Firestore como `foto_url`.
 * @property latitud Coordenada de latitud de la ubicación de la biblioteca.
 * @property longitud Coordenada de longitud de la ubicación de la biblioteca.
 * @property horario Mapa que representa los horarios de apertura. La clave suele ser el día de la semana
 * y el valor una lista con los rangos horarios de apertura.
 */
data class Biblioteca(
    val id: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val accesibilidad: Boolean = false,
    val email: String = "",
    val descripcion: String = "",

    @get:PropertyName("foto_url")
    @set:PropertyName("foto_url")
    var fotoUrl: String = "",

    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val horario: Map<String, List<String>> = emptyMap()
)