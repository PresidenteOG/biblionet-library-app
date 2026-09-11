package com.biblionet.data.repository.book

import android.util.Log
import com.biblionet.data.model.book.Comentario
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log

@Singleton
class ComentarioRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val comentariosCollection = firestore.collection("comentarios")

    suspend fun getComentarios(libroId: String): List<Comentario> {
        return try {
            comentariosCollection
                .whereEqualTo("libro_id", libroId)
                .get().await()
                .toObjects(Comentario::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addComentario(comentario: Comentario): Result<Unit> = try {
        comentariosCollection
            .document(comentario.comentario_id)
            .set(comentario)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateComentario(comentario: Comentario, nuevoTexto: String, nuevasEstrellas: Int): Result<Unit> = try {
        val snapshot = comentariosCollection
            .whereEqualTo("comentario_id", comentario.comentario_id)
            .get().await()

        if (!snapshot.isEmpty) {
            val docId = snapshot.documents[0].id
            comentariosCollection.document(docId).update(
                "comentario_texto", nuevoTexto,
                "estrellas", nuevasEstrellas,
                "fecha", System.currentTimeMillis()
            ).await()
            Result.success(Unit)
        } else {
            Result.failure(Exception("Comentario no encontrado"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }



    suspend fun deleteComentario(comentario: Comentario): Result<Unit> = try {
        val id = comentario.comentario_id

        // Log de inicio: Verificamos qué ID estamos intentando borrar
        Log.d("FirestoreDelete", "Intentando borrar comentario con ID: '$id'")

        if (id.isBlank()) {
            Log.e("FirestoreDelete", "ERROR: El ID del comentario está vacío. No se puede borrar.")
            Result.failure(Exception("ID de comentario vacío"))
        } else {
            comentariosCollection.document(id).delete().await()

            // Log de éxito
            Log.i("FirestoreDelete", "Éxito: Comentario $id eliminado de Firestore.")
            Result.success(Unit)
        }
    } catch (e: Exception) {
        // Log de error detallado
        Log.e("FirestoreDelete", "Fallo al borrar comentario: ${e.message}", e)
        Result.failure(e)
    }

    suspend fun toggleCensura(comentario: Comentario): Result<Unit> = try {
        val snapshot = comentariosCollection
            .whereEqualTo("comentario_id", comentario.comentario_id)
            .get().await()

        if (!snapshot.isEmpty) {
            val doc = snapshot.documents[0]
            val censuradoActual = doc.getBoolean("censurado") ?: false
            comentariosCollection.document(doc.id).update("censurado", !censuradoActual).await()
            Result.success(Unit)
        } else {
            Result.failure(Exception("Comentario no encontrado"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
