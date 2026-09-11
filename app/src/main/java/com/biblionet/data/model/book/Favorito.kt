package com.biblionet.data.model.book

/**
 * Representa un libro marcado como favorito por un usuario.
 *
 * Se utiliza para almacenar la relación entre usuarios y libros favoritos
 * en la base de datos (Firestore).
 *
 * @property id Identificador único del favorito. Normalmente se genera como
 * usuarioId_libroId para evitar duplicados.
 * @property usuario_id Identificador del usuario que añadió el libro a favoritos.
 * @property libro_id Identificador del libro marcado como favorito.
 * @property fecha_agregado Fecha en la que el libro fue añadido a favoritos,
 * expresada en milisegundos desde epoch.
 */
data class Favorito(
    val id: String = "", // usuarioId_libroId para evitar duplicados
    val usuario_id: String = "",
    val libro_id: String = "",
    val fecha_agregado: Long = System.currentTimeMillis()
)