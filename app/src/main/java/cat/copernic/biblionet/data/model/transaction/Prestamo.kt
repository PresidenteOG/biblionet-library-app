package cat.copernic.biblionet.data.model.transaction

/**
 * Representa un préstamo de un libro realizado por un usuario en una biblioteca.
 *
 * Esta clase almacena la información relacionada con el préstamo,
 * incluyendo las fechas de salida, devolución y entrega del libro.
 *
 * @property prestamoid Identificador único del préstamo.
 * @property usuario_id Identificador del usuario que realiza el préstamo.
 * @property libro_id Identificador del libro prestado.
 * @property isbn Código ISBN del libro prestado.
 * @property fecha_salida Fecha en la que el libro fue prestado, en milisegundos desde epoch.
 * @property biblioteca_id Identificador de la biblioteca desde donde se realiza el préstamo.
 * @property fecha_devolucion Fecha prevista para la devolución del libro, en milisegundos desde epoch.
 * @property fecha_entregado Fecha en la que el libro fue realmente devuelto. Puede ser `null` si aún no se ha entregado.
 * @property estado Estado actual del préstamo (por ejemplo: activo, devuelto, retrasado).
 */
data class Prestamo(
    val prestamoid: String = "",
    val usuario_id: String = "",
    val libro_id: String = "",
    val isbn: String = "",
    val fecha_salida: Long = System.currentTimeMillis(),
    val biblioteca_id: String = "",
    val fecha_devolucion: Long = 0,
    val fecha_entregado: Long? = null,
    val estado: String = ""
)