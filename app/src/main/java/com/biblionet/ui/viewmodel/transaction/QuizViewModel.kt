// Archivo: src/main/java/cat/copernic/biblionet/ui/viewmodel/transaction/QuizViewModel.kt
package com.biblionet.ui.viewmodel.transaction

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biblionet.data.model.transaction.Pregunta
import com.biblionet.data.repository.transaction.QuizRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class QuizState { LOADING, PLAYING, FEEDBACK, RESULTS, NO_QUESTIONS }

class QuizViewModel : ViewModel() {

    private val repository = QuizRepository()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var sharedPrefs: SharedPreferences? = null

    var uiState by mutableStateOf(QuizState.LOADING)
        private set
    var timeLeft by mutableStateOf(30)
        private set
    var currentQuestionIndex by mutableStateOf(0)
        private set
    var score by mutableStateOf(0)
        private set
    var selectedOption by mutableStateOf<Int?>(null)
        private set
    var preguntasActivas by mutableStateOf<List<Pregunta>>(emptyList())
        private set

    var isPracticeMode by mutableStateOf(false)
        private set

    var wasPracticeModeThisRound by mutableStateOf(false)
        private set

    var earnedPointsThisRound by mutableStateOf(0)
        private set

    private var timerJob: Job? = null

    fun inicializar(context: Context) {
        sharedPrefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        if (uiState != QuizState.LOADING && uiState != QuizState.NO_QUESTIONS) return

        val user = auth.currentUser
        if (user != null) {
            db.collection("usuarios").document(user.uid).get().addOnSuccessListener { doc ->
                val hoyStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                val ultimaPartidaStr = doc.getString("ultima_partida_str") ?: ""
                isPracticeMode = (hoyStr == ultimaPartidaStr)
                wasPracticeModeThisRound = isPracticeMode

                if (isPracticeMode) {
                    sharedPrefs?.edit()?.putString("last_quiz_date", hoyStr)?.apply()
                    prepararNuevoQuiz(resuming = false)
                } else {
                    // COMPROBAR SI HAY UNA PARTIDA EN CURSO HOY
                    val ongoingDate = sharedPrefs?.getString("ongoing_quiz_date", "") ?: ""
                    prepararNuevoQuiz(resuming = (ongoingDate == hoyStr))
                }
            }.addOnFailureListener { prepararNuevoQuiz(false) }
        } else {
            prepararNuevoQuiz(false)
        }
    }

    fun prepararNuevoQuiz(resuming: Boolean = false) {
        uiState = QuizState.LOADING

        repository.getAllPreguntas { todasLasPreguntas ->
            if (todasLasPreguntas.isEmpty()) {
                uiState = QuizState.NO_QUESTIONS
                return@getAllPreguntas
            }

            val todayString = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val rngDiario = Random(todayString.toLong())

            val preguntasDelDia = if (todasLasPreguntas.size >= 5) {
                todasLasPreguntas.shuffled(rngDiario).take(5)
            } else {
                todasLasPreguntas.shuffled(rngDiario)
            }
            preguntasActivas = preguntasDelDia

            if (resuming) {
                currentQuestionIndex = sharedPrefs?.getInt("ongoing_quiz_index", 0) ?: 0
                score = sharedPrefs?.getInt("ongoing_quiz_score", 0) ?: 0

                // SISTEMA ANTI-TRAMPAS: Calculamos el tiempo real exacto
                val savedEndTime = sharedPrefs?.getLong("question_end_time", 0L) ?: 0L
                if (savedEndTime > 0L) {
                    val remainingMillis = savedEndTime - System.currentTimeMillis()
                    timeLeft = if (remainingMillis > 0) (remainingMillis / 1000).toInt() else 0
                } else {
                    timeLeft = 30
                }

                if (currentQuestionIndex >= preguntasActivas.size) currentQuestionIndex = 0
            } else {
                currentQuestionIndex = 0
                score = 0
                timeLeft = 30
                if (!isPracticeMode) {
                    sharedPrefs?.edit()?.apply {
                        putString("ongoing_quiz_date", todayString)
                        putInt("ongoing_quiz_index", 0)
                        putInt("ongoing_quiz_score", 0)
                    }?.apply()
                }
            }

            earnedPointsThisRound = 0
            selectedOption = null
            uiState = QuizState.PLAYING
            iniciarTemporizador()
        }
    }

    private fun iniciarTemporizador() {
        timerJob?.cancel()

        // Fijamos la hora absoluta a la que DEBE terminar esta pregunta
        val targetEndTime = System.currentTimeMillis() + (timeLeft * 1000L)

        if (!isPracticeMode) {
            sharedPrefs?.edit()?.putLong("question_end_time", targetEndTime)?.apply()
        }

        timerJob = viewModelScope.launch {
            while (timeLeft > 0 && uiState == QuizState.PLAYING) {
                delay(500L) // Chequeo más rápido para sincronizar mejor UI y reloj
                val remaining = targetEndTime - System.currentTimeMillis()
                timeLeft = if (remaining > 0) (remaining / 1000).toInt() else 0
            }
            // Si se acaba el tiempo (incluso si estaba la app cerrada) se marca errónea (-1)
            if (timeLeft <= 0 && uiState == QuizState.PLAYING) {
                comprobarRespuesta(-1)
            }
        }
    }

    fun comprobarRespuesta(indiceSeleccionado: Int) {
        if (uiState != QuizState.PLAYING) return
        timerJob?.cancel()
        selectedOption = indiceSeleccionado
        uiState = QuizState.FEEDBACK

        val preguntaActual = preguntasActivas[currentQuestionIndex]
        if (indiceSeleccionado == preguntaActual.indiceCorrecto) {
            score += 10
            if (!isPracticeMode) sharedPrefs?.edit()?.putInt("ongoing_quiz_score", score)?.apply()
        }

        viewModelScope.launch {
            delay(1500L)
            if (currentQuestionIndex < preguntasActivas.size - 1) {
                currentQuestionIndex++
                timeLeft = 30
                if (!isPracticeMode) {
                    sharedPrefs?.edit()?.putInt("ongoing_quiz_index", currentQuestionIndex)?.apply()
                }
                selectedOption = null
                uiState = QuizState.PLAYING
                iniciarTemporizador()
            } else {
                finalizarQuiz()
            }
        }
    }

    private fun finalizarQuiz() {
        uiState = QuizState.RESULTS
        val user = auth.currentUser

        // Limpiar partida en curso porque hemos acabado
        sharedPrefs?.edit()?.apply {
            remove("ongoing_quiz_date")
            remove("ongoing_quiz_index")
            remove("ongoing_quiz_score")
            remove("question_end_time")
        }?.apply()

        if (user != null && !isPracticeMode) {
            earnedPointsThisRound = score
            isPracticeMode = true

            val hoyStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            sharedPrefs?.edit()?.putString("last_quiz_date", hoyStr)?.apply()

            viewModelScope.launch {
                try {
                    val userRef = db.collection("usuarios").document(user.uid)
                    val doc = userRef.get().await()

                    val rachaActual = doc.getLong("racha")?.toInt() ?: 0
                    val ultimoDiaRacha = doc.getString("ultimo_dia_racha") ?: ""

                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    val ayerStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.time)

                    val updates = hashMapOf<String, Any>(
                        "ultima_partida_str" to hoyStr,
                        "ultima_partida" to FieldValue.serverTimestamp()
                    )

                    // RACHA: Suma siempre +1 si jugaste hoy o ayer. Si saltaste más de 1 día, se reinicia a 1.
                    val nuevaRacha = if (ultimoDiaRacha == ayerStr || ultimoDiaRacha == hoyStr) {
                        rachaActual + 1
                    } else {
                        1
                    }
                    updates["racha"] = nuevaRacha
                    updates["ultimo_dia_racha"] = hoyStr

                    // Puntuación
                    if (score > 0) {
                        val c = Calendar.getInstance()
                        val year = c.get(Calendar.YEAR)
                        val month = c.get(Calendar.MONTH) + 1
                        val week = c.get(Calendar.WEEK_OF_YEAR)

                        updates["puntos_totales"] = FieldValue.increment(score.toLong())
                        updates["puntos_ano_$year"] = FieldValue.increment(score.toLong())
                        updates["puntos_mes_${year}_${month}"] = FieldValue.increment(score.toLong())
                        updates["puntos_semana_${year}_${week}"] = FieldValue.increment(score.toLong())
                    }
                    userRef.update(updates).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}