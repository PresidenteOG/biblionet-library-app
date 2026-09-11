package com.biblionet.data.repository.transaction

import com.biblionet.data.model.transaction.Prestamo
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrestamoRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val prestamosCollection = firestore.collection("prestamos")
    private val inventarioCollection = firestore.collection("inventario")

    suspend fun crearPrestamo(prestamo: Prestamo): Result<Unit> = try {
        prestamosCollection.document(prestamo.prestamoid).set(prestamo).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun observePrestamosByBiblioteca(bibliotecaId: String, onResult: (List<Prestamo>) -> Unit) {
        prestamosCollection
            .whereEqualTo("biblioteca_id", bibliotecaId)
            .whereIn("estado", listOf("pendiente", "activo", "retrasado"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(Prestamo::class.java) ?: emptyList()
                onResult(list)
            }
    }

    suspend fun procesarAceptar(prestamo: Prestamo): Result<Unit> = try {
        val treintaDiasEnMillis = 30L * 24 * 60 * 60 * 1000
        val nuevaFechaDevolucion = System.currentTimeMillis() + treintaDiasEnMillis

        firestore.runTransaction { transaction ->
            val inventarioQuery = inventarioCollection
                .whereEqualTo("libro_id", prestamo.libro_id)
                .whereEqualTo("biblioteca_id", prestamo.biblioteca_id)
                .get()

            val inventarioSnapshot = Tasks.await(inventarioQuery)
            if (inventarioSnapshot.isEmpty) throw Exception("No se encontró inventario")

            val invDoc = inventarioSnapshot.documents[0]
            val stockActual = invDoc.getLong("stock_disponible") ?: 0

            if (stockActual <= 0) throw Exception("No hay stock disponible")

            transaction.update(prestamosCollection.document(prestamo.prestamoid), mapOf(
                "estado" to "activo",
                "fecha_devolucion" to nuevaFechaDevolucion,
                "fecha_salida" to System.currentTimeMillis()
            ))

            transaction.update(invDoc.reference, "stock_disponible", stockActual - 1)
            null
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun rechazarReserva(prestamoId: String): Result<Unit> = try {
        prestamosCollection.document(prestamoId).update("estado", "rechazado").await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun procesarDevolucion(prestamo: Prestamo): Result<Unit> = try {
        firestore.runTransaction { transaction ->
            val invQuery = inventarioCollection
                .whereEqualTo("libro_id", prestamo.libro_id)
                .whereEqualTo("biblioteca_id", prestamo.biblioteca_id)
                .get()

            val invSnapshot = Tasks.await(invQuery)
            if (invSnapshot.isEmpty) throw Exception("Inventario no encontrado")

            val invDoc = invSnapshot.documents[0]
            val stockActual = invDoc.getLong("stock_disponible") ?: 0

            transaction.update(prestamosCollection.document(prestamo.prestamoid), mapOf(
                "estado" to "devuelto",
                "fecha_entregado" to System.currentTimeMillis()
            ))

            transaction.update(invDoc.reference, "stock_disponible", stockActual + 1)
            null
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
