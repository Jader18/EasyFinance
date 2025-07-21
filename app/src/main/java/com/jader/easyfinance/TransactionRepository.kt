package com.jader.easyfinance

import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date

class TransactionRepository {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    // Obtener la colección de transacciones específica del usuario
    private fun getUserTransactionsCollection() = auth.currentUser?.let { user ->
        db.collection("users").document(user.uid).collection("transactions")
    }

    // Insertar una transacción
    suspend fun insert(transaction: Transaction) {
        val userCollection = getUserTransactionsCollection() ?: return
        val transactionMap = hashMapOf(
            "amount" to transaction.amount,
            "category" to transaction.category,
            "isIncome" to transaction.isIncome,
            "timestamp" to Date()
        )
        userCollection.add(transactionMap).await()
    }

    // Obtener todas las transacciones del usuario como Flow
    fun getAllTransactions(): Flow<List<Transaction>> = flow {
        val userCollection = getUserTransactionsCollection() ?: run {
            emit(emptyList())
            return@flow
        }
        val snapshot = userCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()
        val transactions = snapshot.documents.mapNotNull { doc ->
            try {
                Transaction(
                    id = doc.id.hashCode(),
                    amount = doc.getDouble("amount") ?: 0.0,
                    category = doc.getString("category") ?: "",
                    isIncome = doc.getBoolean("isIncome") ?: false
                )
            } catch (e: Exception) {
                null
            }
        }
        emit(transactions)
    }
}