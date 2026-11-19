package com.example.data

import android.content.Context
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

enum class EventCategory(val id: String, val label: String, val defaultHex: String) {
    OPERACIONES("OPERACIONES", "Operaciones", "#00FFA3"),
    REUNION("REUNION", "Reunión", "#00E5FF"),
    INGENIERIA("INGENIERIA", "Ingeniería", "#3A86FF"),
    SISTEMAS("SISTEMAS", "Sistemas", "#9D4EDD"),
    AUDITORIA("AUDITORIA", "Auditoría", "#FFB84D");

    companion object {
        fun fromId(raw: String?): EventCategory = when (raw) {
            "OPERACIONES", "PERSONAL" -> OPERACIONES
            "REUNION", "TRABAJO" -> REUNION
            "INGENIERIA", "KPOP" -> INGENIERIA
            "SISTEMAS", "SALUD" -> SISTEMAS
            "AUDITORIA", "OTRO" -> AUDITORIA
            else -> OPERACIONES
        }
    }
}

@Keep
@IgnoreExtraProperties
data class CalendarEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val dateKey: String = "",
    val timeLabel: String = "",
    val category: String = EventCategory.OPERACIONES.id,
    val note: String = "",
    val completed: Boolean = false,
    val colorHex: String = "#00FFA3",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class CalendarRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión con Google para sincronizar la agenda.")
    }

    fun observeEvents(userId: String): Flow<List<CalendarEvent>> = flow {
        val path = "users/$userId/calendarEvents"
        emitAll(
            db.collection("users").document(userId).collection("calendarEvents")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CalendarEvent::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<CalendarEvent> { it.dateKey }
                            .thenBy { it.timeLabel.ifBlank { "99:99" } }
                            .thenBy { it.title }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addEvent(
        title: String,
        dateKey: String,
        timeLabel: String,
        category: EventCategory,
        note: String,
        colorHex: String
    ): Result<Unit> {
        val cleanTitle = title.trim().take(100)
        val cleanNote = note.trim().take(300)
        val cleanTime = timeLabel.trim().take(10)
        val cleanHex = normalizeHexForRule(colorHex, category.defaultHex)

        if (cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("El título no puede estar vacío."))
        }
        if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(dateKey)) {
            return Result.failure(IllegalArgumentException("Fecha inválida (formato yyyy-MM-dd)."))
        }

        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/calendarEvents"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("calendarEvents").document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "dateKey" to dateKey,
                "timeLabel" to cleanTime,
                "category" to category.id,
                "note" to cleanNote,
                "completed" to false,
                "colorHex" to cleanHex,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleCompleted(eventId: String, completed: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/calendarEvents/$eventId"
        return try {
            val uid = requireUserId()
            require(eventId.isNotBlank()) { "ID de evento inválido" }
            val docRef = db.collection("users").document(uid)
                .collection("calendarEvents").document(eventId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/calendarEvents/$eventId"
        return try {
            val uid = requireUserId()
            require(eventId.isNotBlank()) { "ID de evento inválido" }
            val docRef = db.collection("users").document(uid)
                .collection("calendarEvents").document(eventId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun normalizeHexForRule(raw: String, fallback: String): String {
        val trimmed = raw.trim()
        val withHash = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
        return if (Regex("^#[0-9A-Fa-f]{6,8}$").matches(withHash)) {
            withHash.uppercase()
        } else {
            fallback
        }
    }
}
