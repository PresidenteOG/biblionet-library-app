// Archivo: QuizRepository.kt
package com.biblionet.data.repository.transaction

import android.util.Log
import com.biblionet.data.model.transaction.Pregunta
import com.google.firebase.firestore.FirebaseFirestore

class QuizRepository {
    private val db = FirebaseFirestore.getInstance()
    private val quizCollection = db.collection("quiz_preguntas")

    fun getAllPreguntas(onResult: (List<Pregunta>) -> Unit) {
        quizCollection.get()
            .addOnSuccessListener { snapshot ->
                val lista = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Pregunta::class.java)?.copy(id = doc.id)
                }
                onResult(lista)
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error al leer preguntas", e)
                onResult(emptyList()) // Si falla, devuelve lista vacía y no bloquea
            }
    }

    fun addPregunta(pregunta: Pregunta, onComplete: (Boolean) -> Unit) {
        val docRef = quizCollection.document()
        val nuevaPregunta = pregunta.copy(id = docRef.id)
        docRef.set(nuevaPregunta)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error al guardar pregunta", e)
                onComplete(false) // Avisamos al ViewModel de que ha fallado
            }
    }

    fun updatePregunta(pregunta: Pregunta, onComplete: (Boolean) -> Unit) {
        if (pregunta.id.isNotEmpty()) {
            quizCollection.document(pregunta.id).set(pregunta)
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Error al actualizar pregunta", e)
                    onComplete(false)
                }
        } else {
            onComplete(false)
        }
    }

    fun deletePregunta(id: String, onComplete: (Boolean) -> Unit) {
        if (id.isNotEmpty()) {
            quizCollection.document(id).delete()
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Error al borrar pregunta", e)
                    onComplete(false)
                }
        } else {
            onComplete(false)
        }
    }
}