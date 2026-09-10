package cat.copernic.biblionet.utils


import cat.copernic.biblionet.data.model.auth.Usuario
import cat.copernic.biblionet.data.model.auth.Role

/**
 * Singleton global para gestionar el estado del usuario logueado.
 */
object GlobalState {
    // El objeto Usuario completo (incluye nombre, foto, rol, etc.)
    var usuario: Usuario? = null

    // Funciones de utilidad para simplificar la lógica en la UI
    fun esAdmin(): Boolean = usuario?.rol == Role.ADMIN
    fun esBibliotecario(): Boolean = usuario?.rol == Role.LIBRARIAN
    fun estaLogueado(): Boolean = usuario != null

    // Limpiar datos al cerrar sesión
    fun finalizarSesion() {
        usuario = null
    }
}

object PenalizacionConfig {
    const val DIAS_POR_RETRASO = 7
    const val DIAS_POR_RETRASO_GRAVE = 15
}