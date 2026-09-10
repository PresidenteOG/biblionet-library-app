// Archivo: cat/copernic/biblionet/data/repository/inventory/InventarioRepository.kt
package cat.copernic.biblionet.data.repository.inventory

// ¡AQUÍ ESTÁ LA LÍNEA MÁGICA QUE FALTABA!
import cat.copernic.biblionet.data.model.inventory.Inventario

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import cat.copernic.biblionet.utils.GlobalState.usuario
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventarioRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val inventarioCollection = firestore.collection("inventario")

    fun observeInventario(): Flow<List<Inventario>> = callbackFlow {
        val subscription = inventarioCollection
            .whereEqualTo("biblioteca_id", usuario?.biblioteca_id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(Inventario::class.java) ?: emptyList()
                trySend(items)
            }
        awaitClose { subscription.remove() }
    }

    fun observeInventarioByBiblioteca(bibliotecaId: String): Flow<List<Inventario>> = callbackFlow {
        val subscription = inventarioCollection
            .whereEqualTo("biblioteca_id", bibliotecaId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(Inventario::class.java) ?: emptyList()
                trySend(items)
            }
        awaitClose { subscription.remove() }
    }

    fun observeInventarioItem(id: String): Flow<Inventario?> = callbackFlow {
        val subscription = inventarioCollection.document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val item = snapshot?.toObject(Inventario::class.java)
                trySend(item)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getInventarioByLibro(libroId: String): List<Inventario> {
        return try {
            inventarioCollection
                .whereEqualTo("libro_id", libroId)
                .get().await()
                .toObjects(Inventario::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun existsInBiblioteca(libroId: String, bibliotecaId: String): Boolean {
        return try {
            val snapshot = inventarioCollection
                .whereEqualTo("libro_id", libroId)
                .whereEqualTo("biblioteca_id", bibliotecaId)
                .get().await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createInventario(inventario: Inventario): Result<String> = try {
        val docRef = inventarioCollection.document()
        val itemWithId = inventario.copy(id = docRef.id)
        docRef.set(itemWithId).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateInventario(inventario: Inventario): Result<Unit> = try {
        inventarioCollection.document(inventario.id).set(inventario).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateInventarioFields(id: String, updates: Map<String, Any>): Result<Unit> = try {
        inventarioCollection.document(id).update(updates).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteInventario(inventarioId: String): Result<Unit> = try {
        inventarioCollection.document(inventarioId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}