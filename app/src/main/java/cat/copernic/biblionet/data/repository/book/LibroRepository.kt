package cat.copernic.biblionet.data.repository.book

import cat.copernic.biblionet.data.model.book.Libro
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldPath
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibroRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val librosCollection = firestore.collection("libros")

    fun observeLibros(
        onSuccess: (List<Libro>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return librosCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Error desconocido")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val libros = snapshot.documents.mapNotNull { document ->
                        document.toObject(Libro::class.java)?.copy(libro_id = document.id)
                    }
                    onSuccess(libros)
                }
            }
    }

    fun observeLibro(libroId: String): Flow<Libro?> = callbackFlow {
        val subscription = librosCollection.document(libroId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val libro = snapshot?.toObject(Libro::class.java)?.copy(libro_id = snapshot.id)
                trySend(libro)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getLibroOnce(libroId: String): Libro? {
        return librosCollection.document(libroId).get().await().toObject(Libro::class.java)?.copy(libro_id = libroId)
    }

    suspend fun createLibro(libro: Libro): Result<String> = try {
        val docRef = librosCollection.document()
        val libroToSave = libro.copy(libro_id = docRef.id)
        docRef.set(libroToSave).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateLibro(libroId: String, data: Map<String, Any>): Result<Unit> = try {
        librosCollection.document(libroId).update(data).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteLibro(libroId: String): Result<Unit> = try {
        // 1. Verificar Inventario
        val inventorySnapshot = firestore.collection("inventario")
            .whereEqualTo("libro_id", libroId)
            .limit(1).get().await()

        if (!inventorySnapshot.isEmpty) {
            // Devolvemos un código de error, no una frase
            Result.failure(Exception("ERR_HAS_INVENTORY"))
        } else {
            // 2. Verificar Préstamos Activos
            val prestamosSnapshot = firestore.collection("prestamos")
                .whereEqualTo("libro_id", libroId)
                .limit(1).get().await()

            if (!prestamosSnapshot.isEmpty) {
                Result.failure(Exception("ERR_HAS_LOANS"))
            } else {
                // 3. Verificar Historial de Reservas (Aviso)
                val historySnapshot = firestore.collection("historial_reservas")
                    .whereEqualTo("libro_id", libroId)
                    .limit(1).get().await()

                if (!historySnapshot.isEmpty) {
                    Result.failure(Exception("ERR_HAS_HISTORY"))
                } else {
                    librosCollection.document(libroId).delete().await()
                    Result.success(Unit)
                }
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateLibroRating(libroId: String, calificacion: String, estrellas: Double): Result<Unit> = try {
        val updates = mapOf(
            "calificacion" to calificacion,
            "estrellas" to estrellas
        )
        librosCollection.document(libroId).update(updates).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getLibroNames(libroIds: List<String>): Map<String, String> {
        if (libroIds.isEmpty()) return emptyMap()
        return try {
            val snapshot = librosCollection
                .whereIn(FieldPath.documentId(), libroIds)
                .get().await()
            snapshot.documents.associate { it.id to (it.getString("titulo") ?: "Sin título") }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // Favoritos logic (could be in a separate repo, but keeping it here for simplicity as requested)
    suspend fun isFavorite(userId: String, libroId: String): Boolean {
        return try {
            firestore.collection("favoritos").document("${userId}_${libroId}").get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleFavorite(userId: String, libroId: String): Boolean {
        val docRef = firestore.collection("favoritos").document("${userId}_${libroId}")
        val snapshot = docRef.get().await()
        return if (snapshot.exists()) {
            docRef.delete().await()
            false
        } else {
            val data = mapOf(
                "usuario_id" to userId,
                "libro_id" to libroId,
                "fecha_agregado" to System.currentTimeMillis()
            )
            docRef.set(data).await()
            true
        }
    }
}
