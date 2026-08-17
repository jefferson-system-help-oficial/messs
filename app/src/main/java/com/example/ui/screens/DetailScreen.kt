package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PoolTaskEntity
import com.example.ui.layout.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PoolViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    taskId: Long,
    viewModel: PoolViewModel,
    onNavigateBack: () -> Unit
) {
    val darkBg = Color(0xFF0D1117)
    val cardBg = Color(0xFF161B22)
    val textWhite = Color(0xFFF0F6FC)
    val textGray = Color(0xFF8B949E)
    val vibrantBlue = Color(0xFF29B6F6)
    val borderGray = Color(0xFF30363D)

    var task by remember { mutableStateOf<PoolTaskEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog controllers
    var showTreatmentDialog by remember { mutableStateOf(false) }
    var showCleaningDialog by remember { mutableStateOf(false) }
    var showAddressDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && task != null) {
            val updated = task!!.copy(photoUri = uri.toString())
            viewModel.updateTask(updated)
            task = updated
            Toast.makeText(context, "Foto da fachada atualizada com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    val afterPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && task != null) {
            val updated = task!!.copy(afterPhotoUri = uri.toString())
            viewModel.updateTask(updated)
            task = updated
            Toast.makeText(context, "Foto pós-limpeza anexada com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    // Observe tasks from viewModel to keep data in sync
    val allTasks by viewModel.tasks.collectAsState()
    LaunchedEffect(taskId, allTasks) {
        val found = allTasks.find { it.id == taskId }
        if (found != null) {
            task = found
            isLoading = false
        } else if (isLoading) {
            val db = com.example.data.AppDatabase.getDatabase(viewModel.getApplication())
            task = db.poolTaskDao().getTaskById(taskId)
            isLoading = false
        }
    }

    Scaffold(
        containerColor = darkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ficha do Atendimento",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = textWhite
                        )
                        Text(
                            text = "Fluxo: Tratamento ➔ Aguardar ➔ Limpeza",
                            fontSize = 11.sp,
                            color = vibrantBlue
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = textWhite)
                    }
                },
                actions = {
                    if (task != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF21262D),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "OS #${task!!.id}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = vibrantBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cardBg)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = vibrantBlue)
            }
        } else if (task == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Atendimento não encontrado.", color = textWhite)
            }
        } else {
            val currentTask = task!!
            val displayStatus = currentTask.displayStatus

            val overallBadgeColor = when (displayStatus) {
                "CONCLUIDA" -> Color(0xFF4CAF50)
                "AGUARDANDO_LIMPEZA" -> Color(0xFFFBC02D)
                "TRATAMENTO_PENDENTE" -> Color(0xFFE53935)
                "ATRASADA" -> Color(0xFFFB8C00)
                "AGENDADA" -> Color(0xFF1E88E5)
                else -> Color(0xFFFBC02D)
            }

            val overallBadgeText = when (displayStatus) {
                "CONCLUIDA" -> "🟢 CONCLUÍDA (TRATADA & LIMPA)"
                "AGUARDANDO_LIMPEZA" -> "🟡 AGUARDANDO LIMPEZA"
                "TRATAMENTO_PENDENTE" -> "🔴 TRATAMENTO PENDENTE"
                "ATRASADA" -> "🟠 ATENDIMENTO ATRASADO"
                "AGENDADA" -> "🔵 AGENDADA"
                else -> displayStatus
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(darkBg)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(appScrollableContentPadding(additionalTop = 16.dp, additionalBottom = 24.dp)),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Client Header Hero Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderGray),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title row & Overall status badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = vibrantBlue.copy(alpha = 0.15f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Pool,
                                            contentDescription = null,
                                            tint = vibrantBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentTask.clientName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = textWhite,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentTask.serviceType,
                                        fontSize = 11.sp,
                                        color = vibrantBlue,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = overallBadgeColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, overallBadgeColor)
                            ) {
                                Text(
                                    text = overallBadgeText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = overallBadgeColor
                                )
                            }
                        }

                        // Photo of Location / Pool Facade Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0D1117)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentTask.photoUri.isNotBlank()) {
                                AsyncImage(
                                    model = currentTask.photoUri,
                                    contentDescription = "Foto da Fachada do Local",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = textGray, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Nenhuma foto anexada", fontSize = 11.sp, color = textGray)
                                }
                            }

                            // Photo Picker Button Overlay
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.70f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clickable { galleryLauncher.launch("image/*") }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Alterar Foto", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Address & GPS row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0D1117))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = currentTask.address,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textWhite
                                    )
                                    Text(
                                        text = "Coordenadas: %.4f, %.4f".format(currentTask.latitude, currentTask.longitude),
                                        fontSize = 11.sp,
                                        color = textGray
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { showAddressDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, vibrantBlue),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ajustar", fontSize = 11.sp, color = vibrantBlue, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Dual Status Indicator Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Water State Badge
                            val isWaterOk = currentTask.isWaterTreated
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isWaterOk) Color(0xFF1B5E20).copy(alpha = 0.3f) else Color(0xFFB71C1C).copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, if (isWaterOk) Color(0xFF4CAF50) else Color(0xFFE53935)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (isWaterOk) Icons.Default.CheckCircle else Icons.Default.WaterDrop,
                                            contentDescription = null,
                                            tint = if (isWaterOk) Color(0xFF4CAF50) else Color(0xFFE53935),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "1. Estado da Água",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textGray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isWaterOk) "🟢 Tratada" else "🔴 Precisa Tratar",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isWaterOk) Color(0xFF4CAF50) else Color(0xFFE53935)
                                    )
                                    if (currentTask.treatmentDate.isNotBlank()) {
                                        Text(
                                            text = currentTask.treatmentDate,
                                            fontSize = 9.sp,
                                            color = textGray
                                        )
                                    }
                                }
                            }

                            // Cleaning State Badge
                            val isCleanOk = currentTask.isCleaningDone
                            val isWaitingClean = currentTask.cleaningState == "AGUARDANDO_LIMPEZA"
                            val cleanColor = when {
                                isCleanOk -> Color(0xFF4CAF50)
                                isWaitingClean -> Color(0xFFFBC02D)
                                else -> Color(0xFFE53935)
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = cleanColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, cleanColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (isCleanOk) Icons.Default.CheckCircle else Icons.Default.CleaningServices,
                                            contentDescription = null,
                                            tint = cleanColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "2. Estado da Limpeza",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textGray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when {
                                            isCleanOk -> "🟢 Limpa & Concluída"
                                            isWaitingClean -> "🟡 Aguardando Limpeza"
                                            else -> "🔴 Limpeza Pendente"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = cleanColor
                                    )
                                    if (currentTask.cleaningScheduledDate.isNotBlank() && !isCleanOk) {
                                        Text(
                                            text = "Limpar: ${currentTask.cleaningScheduledDate}",
                                            fontSize = 9.sp,
                                            color = Color(0xFFFBC02D),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else if (currentTask.cleaningCompletedDate.isNotBlank()) {
                                        Text(
                                            text = currentTask.cleaningCompletedDate,
                                            fontSize = 9.sp,
                                            color = textGray
                                        )
                                    }
                                }
                            }
                        }

                        // Navigation & External Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val encoded = Uri.encode(currentTask.address)
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded"))
                                    try { context.startActivity(intent) } catch (e: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937), contentColor = vibrantBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Navegar GPS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    // Generate WhatsApp Report
                                    val report = """
                                        *SÓ PISCINAS - Relatório de Atendimento*
                                        👤 *Cliente:* ${currentTask.clientName}
                                        📍 *Endereço:* ${currentTask.address}
                                        🏊 *Volume:* ${String.format("%,d", currentTask.poolSizeLiters)} L
                                        
                                        💧 *ESTADO DA ÁGUA:* ${if (currentTask.isWaterTreated) "🟢 Tratada" else "🔴 Pendente"}
                                        🧪 *pH:* ${currentTask.phLevel} | *Cloro:* ${currentTask.chlorineLevel} ppm
                                        💊 *Produtos:* ${currentTask.productsApplied.ifBlank { "Químicos de rotina" }}
                                        
                                        🧹 *ESTADO DA LIMPEZA:* ${if (currentTask.isCleaningDone) "🟢 Concluída" else "🟡 Agendada (${currentTask.cleaningScheduledDate})"}
                                        ${if (currentTask.cleaningDurationMinutes > 0) "⏱️ *Tempo de Limpeza:* ${currentTask.cleaningDurationMinutes} min" else ""}
                                        ${if (currentTask.cleaningTasksDone.isNotBlank()) "✅ *Serviços Realizados:* ${currentTask.cleaningTasksDone}" else ""}
                                        
                                        👨‍🔧 *Técnico Responsável:* ${currentTask.technicianName}
                                        📝 *Observações:* ${currentTask.notes.ifBlank { "Tudo em ordem na casa de máquinas." }}
                                    """.trimIndent()

                                    val waIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(report)}")
                                    }
                                    try { context.startActivity(waIntent) } catch (e: Exception) {
                                        Toast.makeText(context, "Não foi possível abrir o WhatsApp", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366).copy(alpha = 0.2f), contentColor = Color(0xFF25D366)),
                                border = BorderStroke(1.dp, Color(0xFF25D366)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 2. ETAPA 1: TRATAMENTO QUÍMICO DA ÁGUA
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderGray)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🧪 Etapa 1: Tratamento Químico",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = textWhite
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (currentTask.isWaterTreated) Color(0xFF2E7D32).copy(alpha = 0.3f) else Color(0xFFD32F2F).copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, if (currentTask.isWaterTreated) Color(0xFF4CAF50) else Color(0xFFE53935))
                            ) {
                                Text(
                                    text = if (currentTask.isWaterTreated) "🟢 Tratada" else "🔴 Pendente",
                                    fontSize = 10.sp,
                                    color = if (currentTask.isWaterTreated) Color(0xFF4CAF50) else Color(0xFFE53935),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Diagnosis specs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SpecBadgeItem(
                                icon = Icons.Default.Science,
                                label = "pH Medido",
                                value = "%.1f (Ideal 7.2-7.6)".format(currentTask.phLevel),
                                modifier = Modifier.weight(1f)
                            )
                            SpecBadgeItem(
                                icon = Icons.Default.Opacity,
                                label = "Cloro Livre",
                                value = "%.1f ppm (Ideal 2-4)".format(currentTask.chlorineLevel),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (currentTask.productsApplied.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0D1117),
                                border = BorderStroke(1.dp, Color(0xFF21262D)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("💊 Produtos Aplicados:", fontSize = 11.sp, color = textGray, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(currentTask.productsApplied, fontSize = 12.sp, color = textWhite)
                                }
                            }
                        }

                        // Primary Action Button for Step 1
                        Button(
                            onClick = { showTreatmentDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentTask.isWaterTreated) Color(0xFF1F2937) else vibrantBlue,
                                contentColor = if (currentTask.isWaterTreated) vibrantBlue else Color.Black
                            ),
                            border = if (currentTask.isWaterTreated) BorderStroke(1.dp, vibrantBlue) else null
                        ) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentTask.isWaterTreated) "Atualizar Tratamento & Agendamento" else "💧 Registrar Tratamento Realizado",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 3. ETAPA 2: LIMPEZA FÍSICA DA PISCINA
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderGray)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🧹 Etapa 2: Limpeza Física",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textWhite
                            )

                            val isCleaningDone = currentTask.isCleaningDone
                            val isWaiting = currentTask.cleaningState == "AGUARDANDO_LIMPEZA"
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    isCleaningDone -> Color(0xFF2E7D32).copy(alpha = 0.3f)
                                    isWaiting -> Color(0xFFFBC02D).copy(alpha = 0.25f)
                                    else -> Color(0xFFD32F2F).copy(alpha = 0.3f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isCleaningDone -> Color(0xFF4CAF50)
                                        isWaiting -> Color(0xFFFBC02D)
                                        else -> Color(0xFFE53935)
                                    }
                                )
                            ) {
                                Text(
                                    text = when {
                                        isCleaningDone -> "🟢 Limpa & Concluída"
                                        isWaiting -> "🟡 Aguardando Limpeza"
                                        else -> "🔴 Pendente"
                                    },
                                    fontSize = 10.sp,
                                    color = when {
                                        isCleaningDone -> Color(0xFF4CAF50)
                                        isWaiting -> Color(0xFFFBC02D)
                                        else -> Color(0xFFE53935)
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Schedule info
                        if (currentTask.cleaningScheduledDate.isNotBlank() && !currentTask.isCleaningDone) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFBC02D).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFFFBC02D).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("📅 Próxima Ação Programada:", fontSize = 11.sp, color = textGray)
                                        Text("Limpeza prevista para: ${currentTask.cleaningScheduledDate}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textWhite)
                                    }
                                }
                            }
                        }

                        if (currentTask.cleaningTasksDone.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0D1117),
                                border = BorderStroke(1.dp, Color(0xFF21262D)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("✅ Serviços Executados:", fontSize = 11.sp, color = textGray, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(currentTask.cleaningTasksDone, fontSize = 12.sp, color = textWhite)
                                    if (currentTask.cleaningDurationMinutes > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("⏱️ Duração: ${currentTask.cleaningDurationMinutes} minutos", fontSize = 11.sp, color = vibrantBlue)
                                    }
                                }
                            }
                        }

                        // After Photo Preview if available
                        if (currentTask.afterPhotoUri.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0D1117)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = currentTask.afterPhotoUri,
                                    contentDescription = "Foto da Piscina Limpa",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black.copy(alpha = 0.7f),
                                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                                ) {
                                    Text("📸 Foto pós-limpeza", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }

                        // Primary Action Button for Step 2
                        Button(
                            onClick = { showCleaningDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentTask.isCleaningDone) Color(0xFF2E7D32) else Color(0xFFFBC02D),
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentTask.isCleaningDone) "✅ Limpeza Realizada (Editar)" else "🧹 Registrar Limpeza Física & Concluir",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 4. HISTÓRICO E LINHA DO TEMPO DO ATENDIMENTO
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderGray)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📜 Linha do Tempo do Atendimento",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textWhite
                            )
                            Icon(Icons.Default.History, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(18.dp))
                        }

                        if (currentTask.historyLog.isBlank()) {
                            Text("Nenhum registro cronológico até o momento.", fontSize = 12.sp, color = textGray)
                        } else {
                            val logEntries = currentTask.historyLog.split("\n").filter { it.isNotBlank() }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                logEntries.forEach { entry ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0D1117),
                                        border = BorderStroke(1.dp, Color(0xFF21262D)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = entry,
                                            fontSize = 12.sp,
                                            color = textWhite,
                                            modifier = Modifier.padding(10.dp),
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Notes & Observations Card
                if (currentTask.notes.isNotBlank()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, borderGray)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "📝 Observações da Casa de Máquinas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = textWhite
                            )
                            Text(
                                text = currentTask.notes,
                                fontSize = 12.sp,
                                color = textGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }

            // ==========================================
            // POPUP 1: REGISTRAR TRATAMENTO QUÍMICO & AGENDAR LIMPEZA AUTOMÁTICA
            // ==========================================
            if (showTreatmentDialog && task != null) {
                val currentTask = task!!
                var editPh by remember { mutableStateOf(currentTask.phLevel.toString()) }
                var editCl by remember { mutableStateOf(currentTask.chlorineLevel.toString()) }
                var editProducts by remember { mutableStateOf(currentTask.productsApplied) }
                var selectedDateOption by remember { mutableStateOf("Amanhã") } // "Amanhã", "Em 2 dias", "Data Personalizada", "Hoje (Concluir direto)"
                var customDateInput by remember { mutableStateOf("") }
                var treatmentNotes by remember { mutableStateOf(currentTask.notes) }

                AlertDialog(
                    onDismissRequest = { showTreatmentDialog = false },
                    containerColor = cardBg,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Piscina Tratada!", color = textWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Quando deverá ser feita a limpeza física?",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = vibrantBlue
                            )

                            // Quick scheduling choices
                            val scheduleOptions = listOf(
                                "Amanhã" to "⚡ Amanhã (+1 dia)",
                                "Em 2 dias" to "⏳ Em 2 dias (+2 dias)",
                                "Data Personalizada" to "📅 Outra Data",
                                "Hoje (Concluir direto)" to "✅ Já limpei hoje"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                scheduleOptions.forEach { (key, label) ->
                                    val isSelected = selectedDateOption == key
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) vibrantBlue.copy(alpha = 0.2f) else Color(0xFF0D1117),
                                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) vibrantBlue else Color(0xFF30363D)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedDateOption = key }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { selectedDateOption = key },
                                                colors = RadioButtonDefaults.colors(selectedColor = vibrantBlue)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(label, color = textWhite, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                            }

                            if (selectedDateOption == "Data Personalizada") {
                                OutlinedTextField(
                                    value = customDateInput,
                                    onValueChange = { customDateInput = it },
                                    label = { Text("Data Prevista (ex: 20/08/2026)", color = textGray) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textWhite, unfocusedTextColor = textWhite,
                                        focusedBorderColor = vibrantBlue, unfocusedBorderColor = borderGray
                                    )
                                )
                            }

                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF21262D)))

                            Text("Medições Químicas:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textWhite)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editPh,
                                    onValueChange = { editPh = it },
                                    label = { Text("pH (7.2-7.6)", color = textGray) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textWhite, unfocusedTextColor = textWhite,
                                        focusedBorderColor = vibrantBlue, unfocusedBorderColor = borderGray
                                    )
                                )
                                OutlinedTextField(
                                    value = editCl,
                                    onValueChange = { editCl = it },
                                    label = { Text("Cloro ppm (2-4)", color = textGray) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textWhite, unfocusedTextColor = textWhite,
                                        focusedBorderColor = vibrantBlue, unfocusedBorderColor = borderGray
                                    )
                                )
                            }

                            // Quick product presets
                            Text("Produtos Utilizados:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textWhite)
                            val quickProducts = listOf(
                                "Cloro Choque 400g",
                                "Sulfato Alumínio",
                                "Barrilha Leve",
                                "Clarificante Max",
                                "Algicida Choque"
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(quickProducts) { prod ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF21262D),
                                        border = BorderStroke(1.dp, borderGray),
                                        modifier = Modifier.clickable {
                                            if (editProducts.isBlank()) editProducts = prod
                                            else if (!editProducts.contains(prod)) editProducts = "$editProducts, $prod"
                                        }
                                    ) {
                                        Text("+ $prod", color = vibrantBlue, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = editProducts,
                                onValueChange = { editProducts = it },
                                label = { Text("Produtos adicionados", color = textGray) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textWhite, unfocusedTextColor = textWhite,
                                    focusedBorderColor = vibrantBlue, unfocusedBorderColor = borderGray
                                )
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val phVal = editPh.toFloatOrNull() ?: currentTask.phLevel
                                val clVal = editCl.toFloatOrNull() ?: currentTask.chlorineLevel
                                viewModel.registerTreatmentStep(
                                    task = currentTask,
                                    scheduledOption = selectedDateOption,
                                    customDateStr = customDateInput,
                                    ph = phVal,
                                    cl = clVal,
                                    products = editProducts.trim(),
                                    technician = currentTask.technicianName.split(" ").firstOrNull() ?: "Jefferson",
                                    notes = treatmentNotes.trim()
                                )
                                showTreatmentDialog = false
                                Toast.makeText(context, "Tratamento registrado! Limpeza agendada.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = vibrantBlue)
                        ) {
                            Text("Confirmar & Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTreatmentDialog = false }) {
                            Text("Cancelar", color = textGray)
                        }
                    }
                )
            }

            // ==========================================
            // POPUP 2: REGISTRAR LIMPEZA FÍSICA & CONCLUIR
            // ==========================================
            if (showCleaningDialog && task != null) {
                val currentTask = task!!
                var durationStr by remember { mutableStateOf(if (currentTask.cleaningDurationMinutes > 0) currentTask.cleaningDurationMinutes.toString() else "38") }
                var aspiracao by remember { mutableStateOf(true) }
                var escovacao by remember { mutableStateOf(true) }
                var preFiltro by remember { mutableStateOf(true) }
                var retrolavagem by remember { mutableStateOf(true) }
                var cleaningNotes by remember { mutableStateOf(currentTask.notes) }

                AlertDialog(
                    onDismissRequest = { showCleaningDialog = false },
                    containerColor = cardBg,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Concluir Limpeza Física", color = textWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Serviços Físicos Realizados:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textWhite)

                            // Checkbox items
                            CleaningCheckItem(label = "Aspiração ao fundo / esgoto", checked = aspiracao, onToggle = { aspiracao = !aspiracao })
                            CleaningCheckItem(label = "Escovação de paredes e bordas", checked = escovacao, onToggle = { escovacao = !escovacao })
                            CleaningCheckItem(label = "Limpeza do pré-filtro e skimmer", checked = preFiltro, onToggle = { preFiltro = !preFiltro })
                            CleaningCheckItem(label = "Retrolavagem & enxágue do filtro", checked = retrolavagem, onToggle = { retrolavagem = !retrolavagem })

                            Spacer(modifier = Modifier.height(4.dp))

                            Text("Tempo Gasto no Atendimento:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textWhite)
                            val presetTimes = listOf("20", "35", "45", "60")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                presetTimes.forEach { mins ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (durationStr == mins) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFF0D1117),
                                        border = BorderStroke(1.dp, if (durationStr == mins) Color(0xFF4CAF50) else Color(0xFF30363D)),
                                        modifier = Modifier.clickable { durationStr = mins }
                                    ) {
                                        Text("$mins min", color = if (durationStr == mins) Color(0xFF4CAF50) else textGray, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = durationStr,
                                onValueChange = { durationStr = it },
                                label = { Text("Duração da limpeza (minutos)", color = textGray) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textWhite, unfocusedTextColor = textWhite,
                                    focusedBorderColor = Color(0xFF4CAF50), unfocusedBorderColor = borderGray
                                )
                            )

                            Button(
                                onClick = { afterPhotoLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D), contentColor = textWhite),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (currentTask.afterPhotoUri.isNotBlank()) "Alterar Foto Pós-Limpeza" else "Anexar Foto da Piscina Limpa", fontSize = 11.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val doneList = mutableListOf<String>()
                                if (aspiracao) doneList.add("Aspiração ao fundo")
                                if (escovacao) doneList.add("Escovação de bordas")
                                if (preFiltro) doneList.add("Pré-filtro/Skimmer limpos")
                                if (retrolavagem) doneList.add("Filtro retrolavado")

                                val durVal = durationStr.toIntOrNull() ?: 35
                                viewModel.registerCleaningStep(
                                    task = currentTask,
                                    durationMinutes = durVal,
                                    tasksDone = doneList.joinToString(", "),
                                    technician = currentTask.technicianName.split(" ").firstOrNull() ?: "Jefferson",
                                    notes = cleaningNotes.trim()
                                )
                                showCleaningDialog = false
                                Toast.makeText(context, "Limpeza concluída! Ciclo finalizado com sucesso.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Concluir Atendimento", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCleaningDialog = false }) {
                            Text("Cancelar", color = textGray)
                        }
                    }
                )
            }

            // ==========================================
            // POPUP 3: AJUSTE DE ENDEREÇO & GPS
            // ==========================================
            if (showAddressDialog && task != null) {
                val currentTask = task!!
                var newAddr by remember { mutableStateOf(currentTask.address) }
                var newLat by remember { mutableStateOf(currentTask.latitude.toString()) }
                var newLng by remember { mutableStateOf(currentTask.longitude.toString()) }

                AlertDialog(
                    onDismissRequest = { showAddressDialog = false },
                    containerColor = cardBg,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PinDrop, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ajustar Endereço e GPS", color = textWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Ajuste o endereço ou coordenadas caso a localização esteja incorreta:", fontSize = 12.sp, color = textGray)
                            OutlinedTextField(
                                value = newAddr,
                                onValueChange = { newAddr = it },
                                label = { Text("Endereço Completo", color = textGray) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textWhite, unfocusedTextColor = textWhite,
                                    focusedBorderColor = vibrantBlue, unfocusedBorderColor = borderGray
                                )
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = newLat,
                                    onValueChange = { newLat = it },
                                    label = { Text("Latitude", color = textGray) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textWhite, unfocusedTextColor = textWhite,
                                        focusedBorderColor = vibrantBlue, unfocusedBorderColor = borderGray
                                    )
                                )
                                OutlinedTextField(
                                    value = newLng,
                                    onValueChange = { newLng = it },
                                    label = { Text("Longitude", color = textGray) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textWhite, unfocusedTextColor = textWhite,
                                        focusedBorderColor = vibrantBlue, unfocusedBorderColor = borderGray
                                    )
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val latVal = newLat.toDoubleOrNull() ?: currentTask.latitude
                                val lngVal = newLng.toDoubleOrNull() ?: currentTask.longitude
                                val updated = currentTask.copy(
                                    address = newAddr.trim(),
                                    latitude = latVal,
                                    longitude = lngVal
                                )
                                viewModel.updateTask(updated)
                                task = updated
                                showAddressDialog = false
                                Toast.makeText(context, "Endereço e GPS atualizados!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = vibrantBlue)
                        ) {
                            Text("Salvar Ajuste", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddressDialog = false }) {
                            Text("Cancelar", color = textGray)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CleaningCheckItem(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (checked) Color(0xFF4CAF50).copy(alpha = 0.12f) else Color(0xFF0D1117),
        border = BorderStroke(1.dp, if (checked) Color(0xFF4CAF50) else Color(0xFF30363D)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 12.sp, color = if (checked) Color.White else Color(0xFF8B949E))
        }
    }
}

@Composable
fun SpecBadgeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0D1117),
        border = BorderStroke(1.dp, Color(0xFF21262D)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF8B949E), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, fontSize = 10.sp, color = Color(0xFF8B949E))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF0F6FC),
                maxLines = 1
            )
        }
    }
}
