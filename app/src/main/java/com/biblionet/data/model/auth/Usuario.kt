package com.biblionet.data.model.auth

import com.google.firebase.Timestamp

/**
 * Representa a un usuario dentro del ecosistema de BiblioNet.
 * * Esta clase de datos almacena tanto la información de autenticación y perfil
 * como las estadísticas de gamificación y estado de la cuenta en Firebase.
 *
 * @property uid Identificador único del usuario (proveniente de Firebase Auth).
 * @property nombre Nombre completo o alias del usuario.
 * @property email Correo electrónico principal de contacto.
 * @property rol Nivel de acceso del usuario (por defecto [Role.READER]).
 * @property telefono Número de contacto del usuario.
 * @property biblioteca_id Referencia a la biblioteca física a la que pertenece el usuario.
 * @property foto_url Enlace a la imagen de perfil almacenada en Firebase Storage.
 * @property idioma Código del idioma preferido para la interfaz (por defecto "es").
 * @property fecha_registro Marca de tiempo de cuándo se creó la cuenta.
 * @property favoritos Lista de IDs de libros o recursos marcados como favoritos.
 * @property ultima_partida Fecha y hora de la última actividad en juegos o secciones interactivas.
 * @property bloqueado_hasta Fecha de finalización de una restricción de acceso, si existe.
 * @property puntos_totales Puntuación acumulada mediante el uso de la aplicación.
 * @property forceLogout Flag para obligar al cierre de sesión remoto (útil tras cambios de permisos).
 * @property libros_leidos Contador total de libros completados por el usuario.
 * @property racha Número de días consecutivos que el usuario ha utilizado la aplicación.
 */
data class Usuario(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val rol: Role = Role.READER,
    val telefono: String = "",
    val biblioteca_id: String = "",
    val foto_url: String = "",
    val idioma: String = "es",
    val fecha_registro: Timestamp = Timestamp.now(),
    val favoritos: List<String> = emptyList(),
    val ultima_partida: Timestamp? = null,
    val bloqueado_hasta: Timestamp? = null,
    val puntos_totales: Int = 0,
    val forceLogout: Boolean = false,

    // NUEVOS CAMPOS AÑADIDOS PARA EL PERFIL
    val libros_leidos: Int = 0,
    val racha: Int = 0
)