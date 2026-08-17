package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pool_tasks")
data class PoolTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val clientName: String,
    val address: String,
    val poolSizeLiters: Int,
    val serviceType: String, // ex: "Tratamento & Limpeza Completa", "Limpeza Semanal", "Tratamento de Choque"
    val date: String, // ex: "2026-08-16"
    val timeSlot: String, // ex: "08:30 - 10:00"
    val status: String = "TRATAMENTO_PENDENTE", // "TRATAMENTO_PENDENTE", "AGUARDANDO_LIMPEZA", "CONCLUIDA", "AGENDADA", "ATRASADA" (ou legados "VERMELHO", "AMARELO", "VERDE", "LARANJA")
    val phLevel: Float = 7.2f,
    val chlorineLevel: Float = 2.0f,
    val notes: String = "",
    val technicianName: String = "Jefferson (Só Piscinas)",
    val latitude: Double = -16.6869,
    val longitude: Double = -49.2648,
    val isCompleted: Boolean = false,
    val photoUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),

    // 🌊 1. Estado da Água / Tratamento Químico
    val waterState: String = "PENDENTE", // "PENDENTE", "TRATADA", "EM_DECANTAÇÃO"
    val treatmentDate: String = "", // ex: "16/08 — 08:32"
    val productsApplied: String = "", // ex: "Cloro Granulado 300g, Clarificante 250ml"
    val treatmentTechnician: String = "Jefferson",

    // 🧹 2. Estado da Limpeza Física
    val cleaningState: String = "PENDENTE", // "PENDENTE", "AGUARDANDO_LIMPEZA", "EM_ANDAMENTO", "LIMPA"
    val cleaningScheduledDate: String = "", // ex: "17/08/2026" ou "Amanhã (17/08)"
    val cleaningCompletedDate: String = "", // ex: "17/08 — 09:15"
    val cleaningDurationMinutes: Int = 0, // ex: 38 min
    val cleaningTechnician: String = "Jefferson",
    val cleaningTasksDone: String = "", // ex: "Aspiração ao esgoto, Escovação de paredes, Filtro retrolavado"
    val afterPhotoUri: String = "",

    // 📜 3. Histórico de Etapas do Atendimento (Timeline)
    val historyLog: String = "" // Registros formatados das ações executadas
) {
    // Helper to get normalized status for UI colors and filters
    val displayStatus: String
        get() = when (status) {
            "TRATAMENTO_PENDENTE", "VERMELHO" -> "TRATAMENTO_PENDENTE"
            "AGUARDANDO_LIMPEZA", "AMARELO" -> "AGUARDANDO_LIMPEZA"
            "CONCLUIDA", "VERDE" -> "CONCLUIDA"
            "ATRASADA", "LARANJA" -> "ATRASADA"
            "AGENDADA" -> "AGENDADA"
            else -> if (isCompleted) "CONCLUIDA" else if (waterState == "TRATADA" && cleaningState != "LIMPA") "AGUARDANDO_LIMPEZA" else "TRATAMENTO_PENDENTE"
        }

    val isWaterTreated: Boolean
        get() = waterState == "TRATADA" || waterState == "EM_DECANTAÇÃO" || status == "CONCLUIDA" || status == "VERDE" || status == "AGUARDANDO_LIMPEZA"

    val isCleaningDone: Boolean
        get() = cleaningState == "LIMPA" || isCompleted || status == "CONCLUIDA" || status == "VERDE"
}
