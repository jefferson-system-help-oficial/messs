package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PoolTaskEntity
import com.example.data.PoolTaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PoolViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PoolTaskRepository

    init {
        val dao = AppDatabase.getDatabase(application).poolTaskDao()
        repository = PoolTaskRepository(dao)
        
        // Seed initial sample data representing the real 2-step pool workflow
        viewModelScope.launch {
            dao.getTotalTaskCount().first().let { count ->
                val currentTasks = dao.getAllTasks().first()
                val hasInhumas = currentTasks.any { it.address.contains("Inhumas", ignoreCase = true) }
                val hasGoianira = currentTasks.any { it.address.contains("Goianira", ignoreCase = true) }
                val hasTrindade = currentTasks.any { it.address.contains("Trindade", ignoreCase = true) }

                if (count == 0 || !hasInhumas || !hasGoianira || !hasTrindade) {
                    // Refresh database with the realistic 2-step workflow clients
                    currentTasks.forEach { dao.deleteTaskById(it.id) }

                    val sampleTasks = listOf(
                        // 1. Inhumas: TRATADA, AGUARDANDO LIMPEZA FÍSICA
                        PoolTaskEntity(
                            clientName = "Chácara & Lazer Inhumas (Sr. Roberto)",
                            address = "Av. Bernardo Sayão, Qd. 12, Lt. 04 - Centro, Inhumas - GO",
                            poolSizeLiters = 85000,
                            serviceType = "Tratamento de Choque & Limpeza",
                            date = "2026-08-16",
                            timeSlot = "08:30 - 10:00",
                            status = "AGUARDANDO_LIMPEZA",
                            phLevel = 7.2f,
                            chlorineLevel = 2.5f,
                            notes = "Água quimicamente tratada e decantada. Aguardando aspiração ao esgoto.",
                            technicianName = "Jefferson (Só Piscinas)",
                            latitude = -16.3575,
                            longitude = -49.4975,
                            photoUri = "https://images.unsplash.com/photo-1576013551627-0cc20b96c2a7?auto=format&fit=crop&w=600&q=80",
                            isCompleted = false,
                            waterState = "TRATADA",
                            treatmentDate = "16/08 — 08:32",
                            productsApplied = "Sulfato de Alumínio 500g, Cloro Choque 400g, Barrilha Leve",
                            treatmentTechnician = "Jefferson",
                            cleaningState = "AGUARDANDO_LIMPEZA",
                            cleaningScheduledDate = "17/08/2026",
                            historyLog = "📅 16/08 — 08:32: Tratamento químico realizado por Jefferson (pH: 7.2 | Cloro: 2.5 ppm). Sulfato de Alumínio aplicado. Limpeza física agendada para 17/08."
                        ),

                        // 2. Goianira: TRATAMENTO PENDENTE (Precisa Tratar)
                        PoolTaskEntity(
                            clientName = "Residencial Lago Azul (Goianira)",
                            address = "Rua G-14, Qd. 05, Lt. 10 - Centro, Goianira - GO",
                            poolSizeLiters = 60000,
                            serviceType = "Tratamento Químico & Aspiração",
                            date = "2026-08-16",
                            timeSlot = "10:45 - 12:00",
                            status = "TRATAMENTO_PENDENTE",
                            phLevel = 6.4f,
                            chlorineLevel = 0.5f,
                            notes = "Água turva e esverdeada pós-chuva. Necessita choque químico e regulador de pH.",
                            technicianName = "Jefferson (Só Piscinas)",
                            latitude = -16.4958,
                            longitude = -49.4269,
                            photoUri = "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=600&q=80",
                            isCompleted = false,
                            waterState = "PENDENTE",
                            cleaningState = "PENDENTE",
                            historyLog = "📅 16/08 — 07:00: Atendimento gerado. Aguardando visita e aplicação dos produtos químicos."
                        ),

                        // 3. Trindade: TRATADA & LIMPA (CONCLUÍDA)
                        PoolTaskEntity(
                            clientName = "Hotel & Pousada Portal da Fé (Trindade)",
                            address = "Av. Raimundo de Paulo, nº 450 - Santuário, Trindade - GO",
                            poolSizeLiters = 110000,
                            serviceType = "Manutenção Completa & Clorador",
                            date = "2026-08-16",
                            timeSlot = "13:30 - 15:30",
                            status = "CONCLUIDA",
                            phLevel = 7.4f,
                            chlorineLevel = 2.8f,
                            notes = "Ciclo concluído. Piscina cristalina, dosador reabastecido e filtro limpo.",
                            technicianName = "Jefferson (Só Piscinas)",
                            latitude = -16.6433,
                            longitude = -49.4889,
                            photoUri = "https://images.unsplash.com/photo-1572331165267-854da2b10ccc?auto=format&fit=crop&w=600&q=80",
                            isCompleted = true,
                            waterState = "TRATADA",
                            treatmentDate = "15/08 — 14:00",
                            productsApplied = "Cloro Granulado 600g, Algicida de Manutenção",
                            treatmentTechnician = "Jefferson",
                            cleaningState = "LIMPA",
                            cleaningCompletedDate = "16/08 — 09:15",
                            cleaningDurationMinutes = 38,
                            cleaningTechnician = "Jefferson",
                            cleaningTasksDone = "Aspiração ao filtro, Escovação de bordas e paredes, Skimmers esvaziados",
                            historyLog = "📅 15/08 — 14:00: Tratamento químico aplicado por Jefferson.\n📅 16/08 — 09:15: Limpeza física concluída em 38 min por Jefferson. Ciclo semanal finalizado com sucesso!"
                        )
                    )
                    sampleTasks.forEach { repository.insertTask(it) }
                }
            }
        }
    }

    private val _selectedFilter = MutableStateFlow<String?>(null) // null = Todos, "TRATAMENTO_PENDENTE", "AGUARDANDO_LIMPEZA", "CONCLUIDA", "ATRASADA", "AGENDADA"
    val selectedFilter: StateFlow<String?> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val tasks: StateFlow<List<PoolTaskEntity>> = combine(
        repository.allTasks,
        _selectedFilter,
        _searchQuery
    ) { allTasks, filter, query ->
        allTasks.filter { task ->
            val displayStat = task.displayStatus
            val matchesFilter = when {
                filter == null || filter == "TODOS" || filter.isBlank() -> true
                filter == "TRATAMENTO_PENDENTE" || filter == "VERMELHO" -> displayStat == "TRATAMENTO_PENDENTE" || (!task.isWaterTreated && !task.isCompleted)
                filter == "AGUARDANDO_LIMPEZA" || filter == "AMARELO" -> displayStat == "AGUARDANDO_LIMPEZA" || (task.isWaterTreated && !task.isCleaningDone)
                filter == "CONCLUIDA" || filter == "VERDE" -> displayStat == "CONCLUIDA" || (task.isWaterTreated && task.isCleaningDone)
                filter == "ATRASADA" || filter == "ATRASADO" || filter == "LARANJA" -> displayStat == "ATRASADA"
                filter == "AGENDADA" -> displayStat == "AGENDADA"
                filter == "LIMPEZA" -> task.serviceType.contains("Limpeza", ignoreCase = true) || task.serviceType.contains("Aspir", ignoreCase = true)
                filter == "TRATAMENTO" -> task.serviceType.contains("Tratamento", ignoreCase = true) || task.serviceType.contains("Choque", ignoreCase = true)
                else -> task.status.equals(filter, ignoreCase = true) || displayStat.equals(filter, ignoreCase = true)
            }
            val matchesQuery = query.isBlank() ||
                    task.clientName.contains(query, ignoreCase = true) ||
                    task.address.contains(query, ignoreCase = true) ||
                    task.serviceType.contains(query, ignoreCase = true) ||
                    task.cleaningScheduledDate.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalCount: StateFlow<Int> = repository.totalCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val unfilteredTasks: StateFlow<List<PoolTaskEntity>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val countTratamentoPendente: StateFlow<Int> = repository.allTasks.map { list ->
        list.count { it.displayStatus == "TRATAMENTO_PENDENTE" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val countAguardandoLimpeza: StateFlow<Int> = repository.allTasks.map { list ->
        list.count { it.displayStatus == "AGUARDANDO_LIMPEZA" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val countConcluida: StateFlow<Int> = repository.allTasks.map { list ->
        list.count { it.displayStatus == "CONCLUIDA" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val countAtrasada: StateFlow<Int> = repository.allTasks.map { list ->
        list.count { it.displayStatus == "ATRASADA" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setFilter(status: String?) {
        _selectedFilter.value = if (status == "TODOS") null else status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun insertTask(task: PoolTaskEntity) {
        viewModelScope.launch {
            repository.insertTask(task)
        }
    }

    fun updateTask(task: PoolTaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    /**
     * 🌊 1. REGISTRAR TRATAMENTO REALIZADO
     * Atualiza o estado da água para TRATADA e agenda a limpeza física sem precisar criar outro chamado!
     */
    fun registerTreatmentStep(
        task: PoolTaskEntity,
        scheduledOption: String, // "Amanhã", "Em 2 dias", "Data Personalizada", "Hoje (Concluir direto)"
        customDateStr: String = "",
        ph: Float = task.phLevel,
        cl: Float = task.chlorineLevel,
        products: String = task.productsApplied,
        technician: String = "Jefferson",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val nowStr = SimpleDateFormat("dd/MM — HH:mm", Locale.getDefault()).format(Date())
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            
            val scheduledCleaningDate = when (scheduledOption) {
                "Amanhã" -> {
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
                }
                "Em 2 dias" -> {
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
                }
                "Hoje (Concluir direto)" -> dateStr
                else -> customDateStr.ifBlank { dateStr }
            }

            val isSameDayComplete = (scheduledOption == "Hoje (Concluir direto)")
            val newStatus = if (isSameDayComplete) "CONCLUIDA" else "AGUARDANDO_LIMPEZA"
            val newCleaningState = if (isSameDayComplete) "LIMPA" else "AGUARDANDO_LIMPEZA"

            val newLogEntry = "📅 $nowStr: Tratamento químico realizado por $technician (pH: $ph | Cloro: $cl ppm). Produtos: ${products.ifBlank { "Químicos de rotina" }}. Limpeza física prevista: $scheduledCleaningDate."
            val updatedHistory = if (task.historyLog.isBlank()) newLogEntry else "${task.historyLog}\n$newLogEntry"

            val updated = task.copy(
                status = newStatus,
                waterState = "TRATADA",
                treatmentDate = nowStr,
                treatmentTechnician = technician,
                phLevel = ph,
                chlorineLevel = cl,
                productsApplied = products,
                cleaningState = newCleaningState,
                cleaningScheduledDate = scheduledCleaningDate,
                isCompleted = isSameDayComplete,
                notes = if (notes.isNotBlank()) notes else task.notes,
                historyLog = updatedHistory
            )
            repository.updateTask(updated)
        }
    }

    /**
     * 🧹 2. REGISTRAR LIMPEZA FÍSICA REALIZADA
     * Conclui o ciclo completo de atendimento com registro de tempo, escovação e foto!
     */
    fun registerCleaningStep(
        task: PoolTaskEntity,
        durationMinutes: Int,
        tasksDone: String,
        technician: String = "Jefferson",
        notes: String = "",
        afterPhotoUri: String = ""
    ) {
        viewModelScope.launch {
            val nowStr = SimpleDateFormat("dd/MM — HH:mm", Locale.getDefault()).format(Date())
            val newLogEntry = "📅 $nowStr: Limpeza física concluída por $technician (Duração: ${durationMinutes} min). Serviços: ${tasksDone.ifBlank { "Aspiração e escovação completa" }}. Ciclo concluído com sucesso!"
            val updatedHistory = if (task.historyLog.isBlank()) newLogEntry else "${task.historyLog}\n$newLogEntry"

            val updated = task.copy(
                status = "CONCLUIDA",
                cleaningState = "LIMPA",
                cleaningCompletedDate = nowStr,
                cleaningDurationMinutes = durationMinutes,
                cleaningTechnician = technician,
                cleaningTasksDone = tasksDone,
                isCompleted = true,
                afterPhotoUri = if (afterPhotoUri.isNotBlank()) afterPhotoUri else task.afterPhotoUri,
                notes = if (notes.isNotBlank()) notes else task.notes,
                historyLog = updatedHistory
            )
            repository.updateTask(updated)
        }
    }

    fun toggleComplete(task: PoolTaskEntity) {
        viewModelScope.launch {
            val willBeCompleted = !task.isCompleted
            val nowStr = SimpleDateFormat("dd/MM — HH:mm", Locale.getDefault()).format(Date())
            val newStatus = if (willBeCompleted) "CONCLUIDA" else "TRATAMENTO_PENDENTE"
            val newWater = if (willBeCompleted) "TRATADA" else "PENDENTE"
            val newCleaning = if (willBeCompleted) "LIMPA" else "PENDENTE"
            
            val logText = if (willBeCompleted) {
                "📅 $nowStr: Atendimento marcado como concluído manualmente."
            } else {
                "📅 $nowStr: Atendimento reaberto para tratamento."
            }
            val updatedHistory = if (task.historyLog.isBlank()) logText else "${task.historyLog}\n$logText"

            val updated = task.copy(
                isCompleted = willBeCompleted,
                status = newStatus,
                waterState = newWater,
                cleaningState = newCleaning,
                historyLog = updatedHistory
            )
            repository.updateTask(updated)
        }
    }
}
