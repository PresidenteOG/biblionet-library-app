package com.biblionet.data.model.book

/**
 * Representa un comentario realizado por un usuario sobre un libro.
 *
 * Esta clase se utiliza para almacenar y recuperar comentarios desde Firestore.
 *
 * @property comentario_id Identificador único del comentario generado automáticamente por Firestore.
 * @property libro_id Identificador del libro al que pertenece el comentario.
 * @property usuario_id Identificador del usuario que realizó el comentario.
 * @property estrellas Número de estrellas otorgadas al libro (normalmente entre 1 y 5).
 * @property comentario_texto Texto del comentario escrito por el usuario.
 * @property fecha Fecha en la que se creó el comentario, representada en milisegundos desde epoch.
 * @property censurado Indica si el comentario ha sido censurado o moderado.
 */
data class Comentario(
    val comentario_id: String = "", // ID autogenerado de Firestore
    val libro_id: String = "",
    val usuario_id: String = "",
    val estrellas: Int = 0,
    val comentario_texto: String = "",
    val fecha: Long = System.currentTimeMillis(),
    val censurado: Boolean = false,
)