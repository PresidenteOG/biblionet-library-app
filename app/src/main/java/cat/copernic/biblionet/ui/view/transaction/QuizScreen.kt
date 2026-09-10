// Archivo: cat/copernic/biblionet/ui/view/transaction/QuizScreen.kt
package cat.copernic.biblionet.ui.view.transaction

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cat.copernic.biblionet.R
import cat.copernic.biblionet.ui.viewmodel.transaction.QuizState
import cat.copernic.biblionet.ui.viewmodel.transaction.QuizViewModel
import kotlinx.coroutines.*
import java.util.Locale

fun playRetroGameSound(type: String) {
    CoroutineScope(Dispatchers.IO).launch {
        val tone = ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
        try {
            when (type) {
                "CORRECT" -> { tone.startTone(ToneGenerator.TONE_DTMF_3, 120); delay(120); tone.startTone(ToneGenerator.TONE_DTMF_6, 150) }
                "INCORRECT" -> { tone.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 300) }
                "VICTORY" -> { tone.startTone(ToneGenerator.TONE_DTMF_1, 120); delay(120); tone.startTone(ToneGenerator.TONE_DTMF_3, 120); delay(120); tone.startTone(ToneGenerator.TONE_DTMF_5, 120); delay(120); tone.startTone(ToneGenerator.TONE_DTMF_9, 400) }
            }
        } finally { tone.release() }
    }
}

@Composable
fun QuizScreen(onNavigateBack: () -> Unit, viewModel: QuizViewModel = viewModel()) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    val letras = listOf("A","B","C","D")
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val currentLang = prefs.getString("app_lang", "es") ?: "es"

    LaunchedEffect(Unit) { viewModel.inicializar(context) }

    DisposableEffect(context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = when(currentLang) { "ca" -> Locale("ca","ES"); "en" -> Locale.US; else -> Locale("es","ES") }
                tts?.language = locale
            }
        }
        onDispose { tts?.stop(); tts?.shutdown() }
    }

    val state = viewModel.uiState

    LaunchedEffect(state) {
        when(state) {
            QuizState.FEEDBACK -> {
                val preguntaActual = viewModel.preguntasActivas.getOrNull(viewModel.currentQuestionIndex)
                if(preguntaActual != null) {
                    if(viewModel.selectedOption == preguntaActual.indiceCorrecto) playRetroGameSound("CORRECT")
                    else playRetroGameSound("INCORRECT")
                }
            }
            QuizState.RESULTS -> playRetroGameSound("VICTORY")
            else -> {}
        }
    }

    if(state == QuizState.RESULTS) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A237E)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏆", fontSize = 60.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Quiz Terminado", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))

                // CORRECCIÓN MENSAJE FINAL
                if (!viewModel.wasPracticeModeThisRound) {
                    Text("+${viewModel.score} pts", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                } else {
                    Text(stringResource(R.string.practica_completada), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Green, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(30.dp))
                Button(onClick = onNavigateBack) { Text("Volver") }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = { viewModel.prepararNuevoQuiz() }) {
                    Icon(Icons.Default.Refresh,null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Jugar Práctica")
                }
            }
        }
        return
    }

    if(viewModel.preguntasActivas.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val preguntaActual = viewModel.preguntasActivas.getOrNull(viewModel.currentQuestionIndex) ?: return
    val textoPregunta = preguntaActual.getTextoIdioma(currentLang)
    val opcionesPregunta = preguntaActual.getOpcionesIdioma(currentLang)

    Scaffold(containerColor = Color(0xFFF4F6FB)) { padding ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(padding)) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack,null,tint = Color.White) }
                        Text("Quiz", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        IconButton(onClick = {
                            val textoOpciones = opcionesPregunta.mapIndexed { i, op -> "${letras[i]}. $op" }.joinToString(". ")
                            tts?.speak(textoPregunta, TextToSpeech.QUEUE_FLUSH, null, "Pregunta")
                            tts?.playSilentUtterance(800, TextToSpeech.QUEUE_ADD, "pause")
                            tts?.speak(textoOpciones, TextToSpeech.QUEUE_ADD, null, "Opciones")
                        }) { Icon(Icons.Default.VolumeUp,null,tint = Color.White) }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${viewModel.currentQuestionIndex+1}/${viewModel.preguntasActivas.size}", fontSize = 14.sp, color = Color.LightGray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${viewModel.timeLeft}s", fontSize = 14.sp, color = Color(0xFFF59E0B))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    val progress = (viewModel.currentQuestionIndex+1).toFloat() / viewModel.preguntasActivas.size.toFloat()
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = Color(0xFF7C3AED))
                }
            }

            Text("Q${viewModel.currentQuestionIndex+1}. $textoPregunta", fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(20.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                opcionesPregunta.forEachIndexed { index, opcion ->
                    val isFeedback = viewModel.uiState == QuizState.FEEDBACK
                    val isCorrect = index == preguntaActual.indiceCorrecto
                    val isSelected = index == viewModel.selectedOption

                    val bgColor = when { isFeedback && isCorrect -> Color(0xFFD1FAE5); isFeedback && isSelected && !isCorrect -> Color(0xFFFFE4E6); else -> Color.White }
                    val borderColor = when { isFeedback && isCorrect -> Color(0xFF10B981); isFeedback && isSelected && !isCorrect -> Color(0xFFF43F5E); else -> Color(0xFFE5E7EB) }

                    OutlinedButton(
                        onClick = { if(viewModel.uiState == QuizState.PLAYING) { tts?.stop(); viewModel.comprobarRespuesta(index) } },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(54.dp), shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp,borderColor), colors = ButtonDefaults.outlinedButtonColors(containerColor = bgColor)
                    ) { Text("${letras[index]}. $opcion", fontSize = 15.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}