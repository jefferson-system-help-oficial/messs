package com.example.ui.screens.home.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PoolTaskEntity
import com.example.ui.layout.appScrollableContentPadding
import com.example.ui.screens.home.tasks.TaskListItemCard
import java.text.SimpleDateFormat
import java.util.*

data class DateModel(val year: Int, val month: Int, val day: Int) {
    fun toIsoString(): String = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
}

fun getDaysInMonth(month: Int, year: Int): List<DateModel?> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month)
    cal.set(Calendar.DAY_OF_MONTH, 1)

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday

    val days = mutableListOf<DateModel?>()
    for (i in 1 until firstDayOfWeek) {
        days.add(null)
    }
    for (i in 1..daysInMonth) {
        days.add(DateModel(year, month, i))
    }
    return days
}

fun getTaskStatusType(task: PoolTaskEntity): Int {
    if (task.isCompleted || task.displayStatus == "CONCLUIDA") return 1 // Verde
    if (task.displayStatus == "AGUARDANDO_LIMPEZA" || task.cleaningState == "AGUARDANDO_LIMPEZA") return 2 // Amarelo
    if (task.displayStatus == "TRATAMENTO_PENDENTE" || task.displayStatus == "ATRASADA") return 3 // Vermelho
    return 4 // Azul
}

@Composable
fun CalendarioTabScreen(
    tasks: List<PoolTaskEntity>,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onBackToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = Color(0xFF131B2E)
    val vibrantBlue = Color(0xFF1A73E8)
    val textWhite = Color(0xFFF0F4F8)
    val textGray = Color(0xFF94A3B8)

    val todayCal = Calendar.getInstance()
    val todayModel = DateModel(todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH))
    
    var currentYear by remember { mutableStateOf(todayModel.year) }
    var currentMonth by remember { mutableStateOf(todayModel.month) } // 0-11
    var selectedDate by remember { mutableStateOf(todayModel) }

    val monthNames = listOf("JANEIRO", "FEVEREIRO", "MARÇO", "ABRIL", "MAIO", "JUNHO", "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO")
    val weekdays = listOf("DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SÁB")

    // Filter tasks for the selected date
    val selectedIso = selectedDate.toIsoString()
    val dailyTasks = remember(tasks, selectedIso) {
        tasks.filter { it.date == selectedIso || it.cleaningScheduledDate.contains(selectedIso) }
    }

    // Pre-calculate tasks for the entire month to show indicators
    val tasksByIso = remember(tasks) {
        val map = mutableMapOf<String, MutableList<PoolTaskEntity>>()
        tasks.forEach { task ->
            if (task.date.isNotBlank()) {
                map.getOrPut(task.date) { mutableListOf() }.add(task)
            }
            if (task.cleaningScheduledDate.isNotBlank()) {
                val cleaningIso = task.cleaningScheduledDate.take(10) // Basic extraction if format matches
                if (cleaningIso != task.date) {
                    map.getOrPut(cleaningIso) { mutableListOf() }.add(task)
                }
            }
        }
        map
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .displayCutoutPadding()
            .padding(horizontal = 16.dp),
        contentPadding = appScrollableContentPadding(additionalTop = 12.dp, additionalBottom = 16.dp, horizontal = 0.dp)
    ) {
        // --- HEADER ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackToMap,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .border(1.dp, Color(0xFF30363D), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar ao Mapa", tint = textWhite)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Controle Operacional",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textWhite
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateToAdd,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(vibrantBlue)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Novo Agendamento", tint = Color.Black)
                    }
                    IconButton(
                        onClick = onNavigateToProfile,
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
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- CALENDAR MONTH NAVIGATION ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentMonth == 0) { currentMonth = 11; currentYear-- } else { currentMonth-- }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mês Anterior", tint = vibrantBlue)
                }

                Text(
                    text = "${monthNames[currentMonth]} $currentYear",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textWhite
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = cardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D)),
                        modifier = Modifier
                            .clickable {
                                currentYear = todayModel.year
                                currentMonth = todayModel.month
                                selectedDate = todayModel
                            }
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Hoje",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textWhite,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (currentMonth == 11) { currentMonth = 0; currentYear++ } else { currentMonth++ }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo Mês", tint = vibrantBlue)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- CALENDAR GRID ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Weekday Headers
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        weekdays.forEach { day ->
                            Text(
                                text = day,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textGray,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Days Grid
                    val days = getDaysInMonth(currentMonth, currentYear)
                    val rows = days.chunked(7)
                    
                    rows.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            for (i in 0 until 7) {
                                val day = row.getOrNull(i)
                                Box(modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)) {
                                    if (day != null) {
                                        val iso = day.toIsoString()
                                        val isSelected = selectedDate == day
                                        val isToday = todayModel == day
                                        val dayTasks = tasksByIso[iso] ?: emptyList()
                                        val statusSet = dayTasks.map { getTaskStatusType(it) }.toSet()

                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) vibrantBlue.copy(alpha = 0.2f) else Color.Transparent)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) vibrantBlue else if (isToday) Color(0xFF30363D) else Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { selectedDate = day }
                                                .padding(top = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${day.day}",
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) vibrantBlue else if (isToday) Color.White else textWhite
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 4.dp)
                                            ) {
                                                if (statusSet.contains(1)) Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF4CAF50)).padding(horizontal = 1.dp))
                                                if (statusSet.contains(2)) Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFFBC02D)).padding(horizontal = 1.dp))
                                                if (statusSet.contains(3)) Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFE53935)).padding(horizontal = 1.dp))
                                                if (statusSet.contains(4)) Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF1E88E5)).padding(horizontal = 1.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- DAILY REPORT & DASHBOARD ---
        item {
            val total = dailyTasks.size
            val concluidos = dailyTasks.count { getTaskStatusType(it) == 1 }
            val aguardando = dailyTasks.count { getTaskStatusType(it) == 2 }
            val pendentes = dailyTasks.count { getTaskStatusType(it) == 3 }
            val agendados = dailyTasks.count { getTaskStatusType(it) == 4 }

            val isTodaySelected = selectedDate == todayModel
            val dateLabel = if (isTodaySelected) "Resumo de Hoje" else "Relatório: ${String.format(Locale.US, "%02d/%02d/%04d", selectedDate.day, selectedDate.month + 1, selectedDate.year)}"

            Text(
                text = dateLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textWhite
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(text = "🏊 $total", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textWhite)
                            Text(text = "Atendimentos", fontSize = 11.sp, color = textGray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🟢 $concluidos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                            Text(text = "Concluídos", fontSize = 11.sp, color = textGray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🟡 $aguardando", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBC02D))
                            Text(text = "Aguardando", fontSize = 11.sp, color = textGray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "🔴 $pendentes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                            Text(text = "Pendentes", fontSize = 11.sp, color = textGray)
                        }
                    }

                    if (isTodaySelected) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFF21262D))
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Dummy metrics for operational realism as requested
                            val calcKm = total * 5.2
                            val calcHours = total * 0.5
                            val h = calcHours.toInt()
                            val m = ((calcHours - h) * 60).toInt()

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = textGray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "${String.format(Locale.US, "%.1f", calcKm)} km rodados", fontSize = 12.sp, color = textWhite, fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = textGray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "${h}h ${m}min trab.", fontSize = 12.sp, color = textWhite, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Agenda do Dia",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textWhite
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // --- DAILY AGENDA ITEMS ---
        if (dailyTasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = textGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Nenhum cliente agendado",
                            color = textWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Dia livre para prospecção ou folga",
                            color = textGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(dailyTasks, key = { it.id }) { task ->
                TaskListItemCard(
                    task = task,
                    onClick = { onNavigateToDetail(task.id) }
                )
            }
        }
    }
}

