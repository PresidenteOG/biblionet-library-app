package cat.copernic.biblionet.data.model.book

/**
 * Representa una categoría o género literario dentro del sistema BiblioNet.
 * * Esta clase se utiliza para clasificar los libros y facilitar la navegación
 * del usuario a través de diferentes temáticas (ej. Fantasía, Historia, Ciencia Ficción).
 *
 * @property categoriaId Identificador único de la categoría, generalmente generado por Firestore.
 * @property nombre El nombre visible de la categoría (ej. "Novela Negra").
 * @property descripcion Una breve explicación del tipo de contenido que engloba esta categoría.
 * @property imagenUrl Enlace a una imagen representativa o icono de la categoría almacenado en el servidor.
 */
data class Categoria(
    var categoriaId: String = "",
    var nombre: String = "",
    var descripcion: String = "",
    var imagenUrl: String = ""
)