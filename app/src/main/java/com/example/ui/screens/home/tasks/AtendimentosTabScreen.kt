package com.example.ui.screens.home.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PoolTaskEntity
import com.example.ui.layout.appScrollableContentPadding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

/**
 * Tela de gestão de atendimentos (lista de tarefas com filtros, busca e resumo).
 * Exibe os atendimentos do dia com status e permite navegação para detalhes.
 *
 * @param tasks Lista completa de tarefas (PoolTaskEntity)
 * @param searchQuery Texto atual da busca
 * @param selectedFilter Filtro ativo (ex: "TRATAMENTO_PENDENTE", "CONCLUIDA", etc.)
 * @param onSearchChange Callback para alterar a busca
 * @param onFilterChange Callback para alterar o filtro
 * @param onNavigateToAdd Callback para navegar para criação de novo atendimento
 * @param onNavigateToDetail Callback para navegar para detalhes de uma tarefa (recebe o ID)
 * @param onNavigateToProfile Callback para navegar para perfil do técnico
 * @param onBackToMap Callback para voltar para o mapa
 * @param modifier Modifier para estilização externa
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtendimentosTabScreen(
    tasks: List<PoolTaskEntity>,
    searchQuery: String,
    selectedFilter: String?,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToProfile: () -> Unit,
    onBackToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Cores (idealmente viriam de um tema)
    val cardBg = Color(0xFF131B2E)
    val cardBgLight = Color(0xFF18233A)
    val vibrantBlue = Color(0xFF1A73E8)
    val textWhite = Color(0xFFF0F4F8)
    val textGray = Color(0xFF94A3B8)

    // Cores de status
    val red = Color(0xFFE53935)
    val yellow = Color(0xFFFFC107)
    val green = Color(0xFF43A047)
    val orange = Color(0xFFFF8A00)
    val blue = Color(0xFF42A5F5)

    // Data de hoje para comparação (formato ISO)
    val todayIso = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // --- Lógica de status (extraída para funções puras) ---
    fun isPending(task: PoolTaskEntity) = TaskStatusUtils.isPending(task)
    fun isAwaitingCleaning(task: PoolTaskEntity) = TaskStatusUtils.isAwaitingCleaning(task)
    fun isCompleted(task: PoolTaskEntity) = TaskStatusUtils.isCompleted(task)
    fun isOverdue(task: PoolTaskEntity) = TaskStatusUtils.isOverdue(task, todayIso)
    fun isScheduled(task: PoolTaskEntity) = TaskStatusUtils.isScheduled(task)

    // --- Contagens para resumo ---
    val pendingCount = tasks.count { isPending(it) }
    val awaitingCleaningCount = tasks.count { isAwaitingCleaning(it) }
    val completedCount = tasks.count { isCompleted(it) }
    val overdueCount = tasks.count { isOverdue(it) }
    val scheduledCount = tasks.count { isScheduled(it) }

    // --- Filtros disponíveis ---
    val filterCategories = listOf(
        "TODOS" to "📍 Todos",
        "TRATAMENTO_PENDENTE" to "🔴 Precisa Tratar",
        "AGUARDANDO_LIMPEZA" to "🟡 Aguardando Limpeza",
        "CONCLUIDA" to "🟢 Concluídas",
        "ATRASADA" to "🟠 Atrasadas",
        "AGENDADA" to "🔵 Agendadas"
    )

    // --- Lista filtrada (computed com derivedStateOf para evitar recomposições desnecessárias) ---
    val filteredTasks by remember {
        derivedStateOf {
            tasks.filter { task ->
                val matchesSearch = searchQuery.isBlank() ||
                        task.clientName.contains(searchQuery, ignoreCase = true) ||
                        task.address.contains(searchQuery, ignoreCase = true) ||
                        task.serviceType.contains(searchQuery, ignoreCase = true)

                val matchesFilter = when (selectedFilter) {
                    null, "TODOS" -> true
                    "TRATAMENTO_PENDENTE" -> isPending(task)
                    "AGUARDANDO_LIMPEZA" -> isAwaitingCleaning(task)
                    "CONCLUIDA" -> isCompleted(task)
                    "ATRASADA" -> isOverdue(task)
                    "AGENDADA" -> isScheduled(task)
                    else -> true
                }
                matchesSearch && matchesFilter
            }
        }
    }

    // --- UI ---
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .displayCutoutPadding()
            .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 0.dp)
    ) {
        // Cabeçalho com botão de voltar, título e ações
        TaskListHeader(
            taskCount = tasks.size,
            onBack = onBackToMap,
            onAdd = onNavigateToAdd,
            onProfile = onNavigateToProfile,
            cardBg = cardBg,
            vibrantBlue = vibrantBlue,
            textWhite = textWhite,
            textGray = textGray
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Resumo de status (cards horizontais)
        SummaryCardsRow(
            pending = pendingCount,
            awaitingCleaning = awaitingCleaningCount,
            completed = completedCount,
            overdue = overdueCount,
            scheduled = scheduledCount,
            cardBg = cardBg,
            red = red,
            yellow = yellow,
            green = green,
            orange = orange,
            blue = blue
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Campo de busca
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchChange,
            cardBg = cardBg,
            vibrantBlue = vibrantBlue,
            textWhite = textWhite,
            textGray = textGray
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Atalhos rápidos (Hoje, Atrasos)
        QuickShortcuts(
            overdueCount = overdueCount,
            onTodayClick = { onFilterChange("TODOS") },
            onOverdueClick = { onFilterChange("ATRASADA") },
            cardBgLight = cardBgLight,
            orange = orange,
            textWhite = textWhite,
            textGray = textGray,
            vibrantBlue = vibrantBlue
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Chips de filtro
        FilterChipsRow(
            categories = filterCategories,
            selectedFilter = selectedFilter,
            onFilterChange = onFilterChange,
            cardBg = cardBg,
            vibrantBlue = vibrantBlue,
            textGray = textGray
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Cabeçalho da lista (contagem e rótulo)
        TaskListSubHeader(
            filterLabel = getFilterLabel(selectedFilter),
            resultCount = filteredTasks.size,
            textWhite = textWhite,
            textGray = textGray,
            vibrantBlue = vibrantBlue,
            cardBgLight = cardBgLight
        )

             // Conteúdo da lista ou estado vazio
        if (filteredTasks.isEmpty()) {
            EmptyState(
                searchQuery = searchQuery,
                onAdd = onNavigateToAdd,
                textWhite = textWhite,
                textGray = textGray,
                vibrantBlue = vibrantBlue,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        } else {
            TaskList(
                tasks = filteredTasks,
                onTaskClick = onNavigateToDetail
            )
        }
    }
}

// ================================================================================================
// COMPONENTES AUXILIARES
// ================================================================================================

@Composable
private fun TaskListHeader(
    taskCount: Int,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onProfile: () -> Unit,
    cardBg: Color,
    vibrantBlue: Color,
    textWhite: Color,
    textGray: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(cardBg)
                    .border(1.dp, Color(0xFF30363D), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = textWhite)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("Atendimentos", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textWhite)
                Text("$taskCount clientes na sua lista", fontSize = 12.sp, color = textGray)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onAdd,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(vibrantBlue)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Atendimento", tint = Color.Black)
            }
            IconButton(
                onClick = onProfile,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(cardBg)
                    .border(1.dp, Color(0xFF30363D), CircleShape)
            ) {
                Icon(Icons.Default.Person, contentDescription = "Perfil", tint = vibrantBlue, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SummaryCardsRow(
    pending: Int,
    awaitingCleaning: Int,
    completed: Int,
    overdue: Int,
    scheduled: Int,
    cardBg: Color,
    red: Color,
    yellow: Color,
    green: Color,
    orange: Color,
    blue: Color
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item { SummaryCard(value = pending, label = "Precisa tratar", icon = "🔴", color = red, background = cardBg) }
        item { SummaryCard(value = awaitingCleaning, label = "Aguardando", icon = "🟡", color = yellow, background = cardBg) }
        item { SummaryCard(value = completed, label = "Concluídas", icon = "🟢", color = green, background = cardBg) }
        item { SummaryCard(value = overdue, label = "Atrasadas", icon = "🟠", color = orange, background = cardBg) }
        item { SummaryCard(value = scheduled, label = "Agendadas", icon = "🔵", color = blue, background = cardBg) }
    }
}

@Composable
private fun SummaryCard(
    value: Int,
    label: String,
    icon: String,
    color: Color,
    background: Color
) {
    Surface(
        modifier = Modifier.width(112.dp),
        shape = RoundedCornerShape(14.dp),
        color = background,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = value.toString(), color = color, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = label, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    cardBg: Color,
    vibrantBlue: Color,
    textWhite: Color,
    textGray: Color
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Buscar cliente, endereço ou serviço...", color = textGray) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = vibrantBlue) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Limpar busca", tint = textGray)
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = cardBg,
            unfocusedContainerColor = cardBg,
            focusedTextColor = textWhite,
            unfocusedTextColor = textWhite,
            cursorColor = vibrantBlue,
            focusedBorderColor = vibrantBlue,
            unfocusedBorderColor = Color(0xFF30363D),
            focusedPlaceholderColor = textGray,
            unfocusedPlaceholderColor = textGray
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun QuickShortcuts(
    overdueCount: Int,
    onTodayClick: () -> Unit,
    onOverdueClick: () -> Unit,
    cardBgLight: Color,
    orange: Color,
    textWhite: Color,
    textGray: Color,
    vibrantBlue: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable { onTodayClick() },
            shape = RoundedCornerShape(12.dp),
            color = cardBgLight,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Hoje", color = textWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable { onOverdueClick() },
            shape = RoundedCornerShape(12.dp),
            color = if (overdueCount > 0) orange.copy(alpha = 0.12f) else cardBgLight,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (overdueCount > 0) orange.copy(alpha = 0.6f) else Color(0xFF30363D)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (overdueCount > 0) orange else textGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (overdueCount > 0) "$overdueCount atrasadas" else "Sem atrasos",
                    color = if (overdueCount > 0) orange else textGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    categories: List<Pair<String, String>>,
    selectedFilter: String?,
    onFilterChange: (String) -> Unit,
    cardBg: Color,
    vibrantBlue: Color,
    textGray: Color
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(categories) { (filterKey, filterLabel) ->
            val isSelected = if (filterKey == "TODOS") {
                selectedFilter == null || selectedFilter == "TODOS"
            } else {
                selectedFilter == filterKey
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) vibrantBlue.copy(alpha = 0.2f) else cardBg,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) vibrantBlue else Color(0xFF30363D)
                ),
                modifier = Modifier.clickable { onFilterChange(filterKey) }
            ) {
                Text(
                    text = filterLabel,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) vibrantBlue else textGray,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun TaskListSubHeader(
    filterLabel: String,
    resultCount: Int,
    textWhite: Color,
    textGray: Color,
    vibrantBlue: Color,
    cardBgLight: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = filterLabel, color = textWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = "$resultCount resultado(s)", color = textGray, fontSize = 11.sp)
        }
        if (resultCount > 0) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = cardBgLight
            ) {
                Text(
                    text = "$resultCount",
                    color = vibrantBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    searchQuery: String,
    onAdd: () -> Unit,
    textWhite: Color,
    textGray: Color,
    vibrantBlue: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (searchQuery.isNotBlank()) Icons.Default.Search else Icons.Default.Event,
                contentDescription = null,
                tint = textGray,
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "Nenhum atendimento encontrado" else "Nenhum atendimento nesta categoria",
                color = textWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "Tente outro nome, endereço ou termo." else "Você pode cadastrar um novo atendimento.",
                color = textGray,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = vibrantBlue, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Novo Atendimento", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TaskList(
    tasks: List<PoolTaskEntity>,
    onTaskClick: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = appScrollableContentPadding(
            additionalTop = 4.dp,
            additionalBottom = 16.dp,
            horizontal = 0.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(tasks, key = { it.id }) { task ->
            TaskListItemCard(
                task = task,
                onClick = { onTaskClick(task.id) }
            )
        }
    }
}

// ================================================================================================
// FUNÇÃO AUXILIAR PARA RÓTULO DO FILTRO
// ================================================================================================

private fun getFilterLabel(filter: String?): String {
    return when (filter) {
        "TRATAMENTO_PENDENTE" -> "Piscinas que precisam de tratamento"
        "AGUARDANDO_LIMPEZA" -> "Piscinas aguardando limpeza"
        "CONCLUIDA" -> "Atendimentos concluídos"
        "ATRASADA" -> "Atendimentos atrasados"
        "AGENDADA" -> "Atendimentos agendados"
        else -> "Todos os atendimentos"
    }
}