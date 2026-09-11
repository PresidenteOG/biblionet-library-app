// Archivo: cat/copernic/biblionet/data/model/inventory/Libro.kt
package com.biblionet.data.model.inventory

/**
 * Representa la información básica de un libro mostrada en la vista de carrito
 * o en el mapa de la biblioteca.
 *
 * Esta clase contiene únicamente los datos necesarios para mostrar
 * el libro en interfaces simplificadas como listas o vistas de selección.
 *
 * @property id Identificador único del libro.
 * @property titulo Título del libro.
 * @property autor Nombre del autor del libro.
 * @property isbn Código ISBN que identifica el libro.
 * @property portadaUrl URL de la imagen de portada del libro.
 * @property disponible Indica si el libro está disponible para préstamo.
 */
data class LibroCartMapView(
    val id: String = "",
    val titulo: String = "",
    val autor: String = "",
    val isbn: String = "",
    val portadaUrl: String = "",
    val disponible: Boolean = true
)