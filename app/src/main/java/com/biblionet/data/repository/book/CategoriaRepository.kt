// Archivo: cat/copernic/biblionet/data/repository/book/CategoriaRepository.kt
package com.biblionet.data.repository.book

import com.biblionet.data.model.book.Categoria
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CategoriaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val categoriasCollection = db.collection("categorias")

    fun getCategorias(): Flow<List<Categoria>> = callbackFlow {
        val subscription = categoriasCollection.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val lista = snapshot?.toObjects(Categoria::class.java) ?: emptyList()
            trySend(lista)
        }
        awaitClose { subscription.remove() }
    }

    suspend fun saveCategoria(categoria: Categoria) {
        val docRef = if (categoria.categoriaId.isEmpty()) {
            categoriasCollection.document() // Crea nuevo doc
        } else {
            categoriasCollection.document(categoria.categoriaId) // Referencia al existente
        }

        // Sincronizamos el ID del documento con el campo interno
        categoria.categoriaId = docRef.id

        docRef.set(categoria).await()
    }

    suspend fun deleteCategoria(id: String) {
        if (id.isNotEmpty()) {
            categoriasCollection.document(id).delete().await()
        }
    }
}