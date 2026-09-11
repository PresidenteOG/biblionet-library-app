package com.biblionet.data.repository.inventory

import com.biblionet.data.model.library.Biblioteca
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BibliotecaRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val bibliotecasCollection = firestore.collection("bibliotecas")

    suspend fun getBiblioteca(id: String): Biblioteca? {
        return try {
            bibliotecasCollection.document(id).get().await().toObject(Biblioteca::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getBibliotecasByIds(ids: List<String>): Map<String, Biblioteca> {
        if (ids.isEmpty()) return emptyMap()
        return try {
            val snapshot = bibliotecasCollection
                .whereIn(FieldPath.documentId(), ids)
                .get().await()
            snapshot.documents.associate { it.id to (it.toObject(Biblioteca::class.java) ?: Biblioteca()) }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
