// Archivo: cat/copernic/biblionet/ui/viewmodel/admin/AdminQuizViewModel.kt
package cat.copernic.biblionet.ui.viewmodel.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.copernic.biblionet.data.model.transaction.Pregunta
import cat.copernic.biblionet.data.repository.transaction.QuizRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

class AdminQuizViewModel : ViewModel() {
    private val repository = QuizRepository()
    private val db = FirebaseFirestore.getInstance()

    var preguntas by mutableStateOf<List<Pregunta>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    init { cargarPreguntas() }

    fun cargarPreguntas() {
        isLoading = true
        repository.getAllPreguntas { lista ->
            preguntas = lista
            isLoading = false
        }
    }

    fun reiniciarQuizGlobalmente(onSuccess: () -> Unit) {
        isLoading = true
        viewModelScope.launch {
            try {
                val usuariosRef = db.collection("usuarios").get().await()
                val batch = db.batch()

                for (doc in usuariosRef.documents) {
                    batch.update(doc.reference, "ultima_partida_str", FieldValue.delete())
                }
                batch.commit().await()

                db.collection("sistema").document("alertas").set(
                    mapOf(
                        "tipo" to "quiz_reset",
                        "timestamp" to FieldValue.serverTimestamp(),
                        "mensaje" to "¡El administrador ha reiniciado el Quiz Diario! Puedes volver a jugar y ganar puntos."
                    )
                ).await()

                isLoading = false
                onSuccess()
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    // --- MOTOR DE AUTO-TRADUCCIÓN ---
    // Utiliza la API pública de Google Translate para traducir textos dinámicamente
    // --- MOTOR DE AUTO-TRADUCCIÓN MEJORADO ---
    private suspend fun autoTranslate(text: String, targetLang: String): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
                val urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=es&tl=$targetLang&dt=t&q=$encodedText"

                // Disfrazamos la petición para que Google no bloquee la app
                val connection = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonArray = org.json.JSONArray(response)
                    val jsonArray2 = jsonArray.getJSONArray(0)
                    val result = java.lang.StringBuilder()
                    for (i in 0 until jsonArray2.length()) {
                        result.append(jsonArray2.getJSONArray(i).getString(0))
                    }
                    result.toString()
                } else {
                    text // Si Google falla por límite de uso, devolvemos el texto original
                }
            } catch (e: Exception) {
                e.printStackTrace()
                text // Fallback seguro
            }
        }
    }

    fun guardarPregunta(id: String, texto: String, opciones: List<String>, indiceCorrecto: Int, onSuccess: () -> Unit) {
        isLoading = true

        viewModelScope.launch {
            try {
                val preguntaId = id.ifEmpty { UUID.randomUUID().toString() }

                // 1. Ejecutar las traducciones asíncronas para el Texto Principal
                val textoEn = autoTranslate(texto, "en")
                val textoCa = autoTranslate(texto, "ca")

                // 2. Ejecutar las traducciones asíncronas para cada una de las Opciones
                val opcionesEn = opciones.map { autoTranslate(it, "en") }
                val opcionesCa = opciones.map { autoTranslate(it, "ca") }

                // 3. Mapeo EXHAUSTIVO en HashMap para evitar el crash de deserialización.
                val preguntaData = hashMapOf(
                    "id" to preguntaId,
                    "texto" to texto,
                    "texto_es" to texto,
                    "texto_en" to textoEn,
                    "texto_ca" to textoCa,
                    "opciones" to opciones,
                    "opciones_es" to opciones,
                    "opciones_en" to opcionesEn,
                    "opciones_ca" to opcionesCa,
                    "indiceCorrecto" to indiceCorrecto
                )

                // 4. Guardar en Firebase (CORREGIDO A LA COLECCIÓN "quiz_preguntas")
                db.collection("quiz_preguntas").document(preguntaId)
                    .set(preguntaData)
                    .await()

                cargarPreguntas()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun eliminarPregunta(id: String) {
        isLoading = true
        repository.deletePregunta(id) { success ->
            if (success) cargarPreguntas() else isLoading = false
        }
    }
}