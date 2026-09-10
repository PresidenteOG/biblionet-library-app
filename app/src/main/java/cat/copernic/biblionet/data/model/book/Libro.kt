package cat.copernic.biblionet.data.model.book

import com.google.firebase.Timestamp

/**
 * Representa un libro dentro del sistema BiblioNet.
 *
 * Contiene toda la información necesaria para mostrar y gestionar
 * los libros almacenados en Firestore.
 *
 * @property libro_id Identificador único del libro en la base de datos.
 * @property titulo Título del libro.
 * @property autor Nombre del autor del libro.
 * @property isbn Código ISBN que identifica el libro internacionalmente.
 * @property descripcion_corta Breve descripción o resumen del libro.
 * @property descripcion_larga Descripción detallada del contenido del libro.
 * @property imagen_url URL de la imagen o portada del libro.
 * @property fecha_creacion Fecha en la que el libro fue añadido al sistema.
 * @property creado_por Identificador del usuario o administrador que creó el registro.
 * @property categoria_id Identificador de la categoría a la que pertenece el libro.
 * @property idioma Idioma en el que está escrito el libro.
 * @property calificacion Valor textual de la calificación media del libro.
 * @property estrellas Calificación numérica media basada en las valoraciones de los usuarios.
 */
data class Libro(
    val libro_id: String = "",
    val titulo: String = "",
    val autor: String = "",
    val isbn: String = "",
    val descripcion_corta: String = "",
    val descripcion_larga: String = "",
    val imagen_url: String = "",
    val fecha_creacion: Timestamp? = null,
    val creado_por: String = "",
    val categoria_id: String = "",
    val idioma: String = "",
    val calificacion: String = "0.0",
    val estrellas: Double = 0.0
)