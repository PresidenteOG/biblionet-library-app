// Archivo: Pregunta.kt (Ubicación: data/model/transaction/Pregunta.kt)
package com.biblionet.data.model.transaction

/**
 * Representa una pregunta de un cuestionario dentro del sistema.
 *
 * La pregunta puede contener traducciones en varios idiomas para soportar
 * internacionalización (español, catalán e inglés). Si una traducción no
 * está disponible, el sistema utiliza automáticamente un idioma alternativo
 * como respaldo.
 *
 * @property id Identificador único de la pregunta.
 * @property texto Texto original de la pregunta (campo de seguridad o fallback).
 * @property texto_es Texto de la pregunta en español.
 * @property texto_ca Texto de la pregunta en catalán.
 * @property texto_en Texto de la pregunta en inglés.
 * @property opciones Lista original de opciones de respuesta (fallback).
 * @property opciones_es Lista de opciones en español.
 * @property opciones_ca Lista de opciones en catalán.
 * @property opciones_en Lista de opciones en inglés.
 * @property indiceCorrecto Índice de la opción correcta dentro de la lista de opciones.
 */
data class Pregunta(
    val id: String = "",
    val texto: String = "", // Campo original por seguridad
    val texto_es: String = "",
    val texto_ca: String = "",
    val texto_en: String = "",
    val opciones: List<String> = emptyList(), // Campo original por seguridad
    val opciones_es: List<String> = emptyList(),
    val opciones_ca: List<String> = emptyList(),
    val opciones_en: List<String> = emptyList(),
    val indiceCorrecto: Int = 0
) {

    /**
     * Devuelve el texto de la pregunta en el idioma solicitado.
     *
     * Si la traducción no está disponible, se utiliza el siguiente orden de prioridad:
     * 1. Idioma solicitado
     * 2. Español
     * 3. Texto original
     *
     * @param lang Código del idioma ("es", "ca", "en").
     * @return Texto de la pregunta en el idioma más apropiado disponible.
     */
    fun getTextoIdioma(lang: String): String {
        return when (lang) {
            "ca" -> if (texto_ca.isNotBlank()) texto_ca else (if (texto_es.isNotBlank()) texto_es else texto)
            "en" -> if (texto_en.isNotBlank()) texto_en else (if (texto_es.isNotBlank()) texto_es else texto)
            else -> if (texto_es.isNotBlank()) texto_es else texto
        }
    }

    /**
     * Devuelve las opciones de respuesta en el idioma solicitado.
     *
     * Si las opciones traducidas no están disponibles, se utiliza el siguiente orden:
     * 1. Idioma solicitado
     * 2. Español
     * 3. Opciones originales
     *
     * @param lang Código del idioma ("es", "ca", "en").
     * @return Lista de opciones en el idioma más adecuado disponible.
     */
    fun getOpcionesIdioma(lang: String): List<String> {
        return when (lang) {
            "ca" -> if (opciones_ca.isNotEmpty()) opciones_ca else (if (opciones_es.isNotEmpty()) opciones_es else opciones)
            "en" -> if (opciones_en.isNotEmpty()) opciones_en else (if (opciones_es.isNotEmpty()) opciones_es else opciones)
            else -> if (opciones_es.isNotEmpty()) opciones_es else opciones
        }
    }
}