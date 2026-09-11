// Archivo: cat/copernic/biblionet/data/model/inventory/Inventario.kt
package com.biblionet.data.model.inventory

/**
 * Representa el inventario de un libro dentro de una biblioteca.
 *
 * Esta clase almacena información sobre la cantidad total de copias
 * de un libro, el stock disponible para préstamo y su ubicación
 * dentro de la biblioteca.
 *
 * @property id Identificador único del registro de inventario.
 * @property libro_id Identificador del libro asociado a este inventario.
 * @property biblioteca_id Identificador de la biblioteca donde se encuentra el libro.
 * @property numero_copias Número total de copias registradas del libro.
 * @property stock_disponible Número de copias actualmente disponibles para préstamo.
 * @property pasilloEstanteria Ubicación física del libro en la biblioteca (pasillo o estantería).
 * @property disponible Indica si el libro está disponible en la biblioteca.
 */
data class Inventario(
    var id: String = "",
    var libro_id: String = "",
    var biblioteca_id: String = "",
    var numero_copias: Int = 0,
    var stock_disponible: Int = 0,
    var pasilloEstanteria: String = "",
    var disponible: Boolean = false
) {


    /**
     * Actualiza el stock disponible del libro.
     *
     * La cantidad puede ser positiva (añadir copias) o negativa (reducir copias).
     * El stock nunca será menor que 0 ni mayor que el número total de copias.
     *
     * @param cantidad Cantidad a sumar o restar del stock disponible.
     */
    fun actualizarStock(cantidad: Int) {
        val nuevoStock = stock_disponible + cantidad
        stock_disponible = when {
            nuevoStock < 0 -> 0
            nuevoStock > numero_copias -> numero_copias
            else -> nuevoStock
        }
    }
}