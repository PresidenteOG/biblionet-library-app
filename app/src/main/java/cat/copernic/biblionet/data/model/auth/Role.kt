package cat.copernic.biblionet.data.model.auth

/**
 * Define los niveles de acceso y permisos para los usuarios dentro de la aplicación BiblioNet.
 * * Este enum se utiliza para gestionar la lógica de autorización, asegurando que cada
 * usuario solo pueda acceder a las funciones correspondientes a su rol asignado.
 */
enum class Role {
    /**
     * Representa a un usuario estándar de la biblioteca.
     * Sus permisos suelen limitarse a consultar el catálogo y gestionar sus propios préstamos.
     */
    READER,

    /**
     * Representa al personal de la biblioteca (bibliotecarios).
     * Tiene permisos para gestionar la colección de libros, supervisar préstamos y ayudar a los lectores.
     */
    LIBRARIAN,

    /**
     * Representa al administrador del sistema.
     * Tiene acceso total a todas las configuraciones, gestión de usuarios y ajustes del sistema.
     */
    ADMIN
}