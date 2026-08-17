package com.example.ui.screens

import android.widget.Toast
import coil.compose.AsyncImage
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PoolTaskEntity
import com.example.ui.layout.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PoolViewModel
import com.example.util.GoogleMapsResolver
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    viewModel: PoolViewModel,
    onNavigateBack: () -> Unit,
    initialSharedText: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val darkBg = Color(0xFF0D1117)
    val cardBg = Color(0xFF161B22)
    val textWhite = Color(0xFFF0F6FC)
    val textGray = Color(0xFF8B949E)
    val vibrantBlue = Color(0xFF29B6F6)
    val borderGray = Color(0xFF30363D)

    // Form fields
    var clientName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var mapsUrl by remember { mutableStateOf("") }
    var latitude by remember { mutableDoubleStateOf(-16.6869) }
    var longitude by remember { mutableDoubleStateOf(-49.2648) }
    var isExtractingLocation by remember { mutableStateOf(false) }
    var extractedFeedback by remember { mutableStateOf<String?>(null) }
    var importedWhatsAppText by remember { mutableStateOf<String?>(initialSharedText) }

    var poolSizeStr by remember { mutableStateOf("50000") }
    var serviceType by remember { mutableStateOf("Limpeza Semanal de Rotina") }
    var timeSlot by remember { mutableStateOf("09:00 - 10:30") }
    var status by remember { mutableStateOf("AMARELO") } // VERDE, AMARELO, LARANJA, VERMELHO
    var phLevelStr by remember { mutableStateOf("7.2") }
    var chlorineStr by remember { mutableStateOf("2.0") }
    var notes by remember { mutableStateOf("") }
    var technicianName by remember { mutableStateOf("Carlos Silva (Equipe Goiânia)") }
    var photoUri by remember { mutableStateOf("") }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    var showError by remember { mutableStateOf(false) }

    // Helper to process location link extraction
    fun processLocationExtraction(input: String) {
        if (input.isBlank()) return
        isExtractingLocation = true
        extractedFeedback = null

        coroutineScope.launch {
            val result = GoogleMapsResolver.resolveAndExtractLocation(input, context)
            isExtractingLocation = false

            if (result != null) {
                latitude = result.latitude
                longitude = result.longitude

                if (!result.addressText.isNullOrBlank()) {
                    if (address.isBlank() || address.contains("maps") || address.contains("http")) {
                        address = result.addressText
                    }
                } else if (address.isBlank()) {
                    address = "Local via Google Maps (${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)})"
                }

                extractedFeedback = "✅ Pino Plotado com Sucesso!\nLat: ${String.format("%.5f", latitude)} | Lng: ${String.format("%.5f", longitude)}"
                Toast.makeText(context, "Localização extraída e pronta para o Mapa!", Toast.LENGTH_SHORT).show()
            } else {
                extractedFeedback = "⚠️ Não foi possível extrair coordenadas automáticas. Verifique o link ou digite o endereço manualmente."
            }
        }
    }

    // Smart WhatsApp Shared Content Parser
    fun parseAndApplySharedContent(text: String) {
        if (text.isBlank()) return
        importedWhatsAppText = text

        // Regex for URL
        val urlRegex = "(https?://[^\\s]+|geo:[^\\s]+)".toRegex()
        val match = urlRegex.find(text)

        if (match != null) {
            val foundUrl = match.value
            mapsUrl = foundUrl
            val remainingText = text.replace(foundUrl, "").trim()

            if (remainingText.isNotBlank()) {
                if (clientName.isBlank()) {
                    // Clean prefix if user wrote "Cliente: Sr. Roberto" or similar
                    val cleanName = remainingText
                        .replace("(?i)cliente:?".toRegex(), "")
                        .replace("(?i)local:?".toRegex(), "")
                        .trim()
                    clientName = cleanName.ifBlank { "Cliente WhatsApp" }
                }
            } else if (clientName.isBlank()) {
                clientName = "Cliente do WhatsApp"
            }
            processLocationExtraction(foundUrl)
        } else {
            // Text only (Address or Client description)
            if (address.isBlank()) {
                address = text.replace("(?i)endereço:?".toRegex(), "").trim()
            }
            if (clientName.isBlank()) {
                clientName = "Cliente do WhatsApp"
            }
            Toast.makeText(context, "Endereço recebido do WhatsApp preenchido!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(initialSharedText) {
        if (!initialSharedText.isNullOrBlank()) {
            parseAndApplySharedContent(initialSharedText)
        }
    }

    Scaffold(
        containerColor = darkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cadastrar Cliente & Piscina",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = textWhite
                        )
                        Text(
                            text = "Cadastre cliente, link do Google Maps e dados técnicos",
                            fontSize = 11.sp,
                            color = textGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = textWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cardBg)
            )
        }
    ) { innerPadding ->
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
            // WhatsApp Imported Banner
            if (!importedWhatsAppText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF25D366).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF25D366)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF25D366),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📲 Dados Importados do WhatsApp / Compartilhamento",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = textWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = importedWhatsAppText!!,
                                fontSize = 11.sp,
                                color = textGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            // Priority & Status Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderGray)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Status Inicial / Prioridade",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textWhite
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DarkStatusColorOption(
                            color = PoolGreen,
                            title = "Verde",
                            subtitle = "Limpa / Ok",
                            isSelected = status == "VERDE",
                            onClick = { status = "VERDE" },
                            modifier = Modifier.weight(1f)
                        )
                        DarkStatusColorOption(
                            color = PoolYellow,
                            title = "Amarelo",
                            subtitle = "Rotina",
                            isSelected = status == "AMARELO",
                            onClick = { status = "AMARELO" },
                            modifier = Modifier.weight(1f)
                        )
                        DarkStatusColorOption(
                            color = PoolOrange,
                            title = "Laranja",
                            subtitle = "Atenção",
                            isSelected = status == "LARANJA",
                            onClick = { status = "LARANJA" },
                            modifier = Modifier.weight(1f)
                        )
                        DarkStatusColorOption(
                            color = PoolRed,
                            title = "Vermelho",
                            subtitle = "Urgente",
                            isSelected = status == "VERMELHO",
                            onClick = { status = "VERMELHO" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Client & Location Data Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderGray)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "👤 Dados do Cliente & Endereço",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = vibrantBlue.copy(0.15f)
                        ) {
                            Text(
                                text = "Cadastro de Piscineiro",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = vibrantBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Client Name
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it; showError = false },
                        label = { Text("Nome do Cliente / Condomínio / Local *", color = textGray) },
                        placeholder = { Text("Ex: Res. Aldeia do Vale / Sr. Roberto", color = textGray.copy(0.4f)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = vibrantBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        isError = showError && clientName.isBlank(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = darkBg,
                            unfocusedContainerColor = darkBg,
                            focusedTextColor = textWhite,
                            unfocusedTextColor = textWhite,
                            focusedBorderColor = vibrantBlue,
                            unfocusedBorderColor = borderGray
                        )
                    )

                    // Google Maps Short Link Resolver Input Block
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0D1117),
                        border = BorderStroke(1.dp, Color(0xFF21262D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false).padding(end = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Map,
                                        contentDescription = null,
                                        tint = vibrantBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Importar do Google Maps",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row {
                                    TextButton(
                                        onClick = {
                                            val sampleMsg = "Cliente: Res. Alphaville Araguaia - Casa 18 - https://maps.app.goo.gl/wE9K2j3x"
                                            parseAndApplySharedContent(sampleMsg)
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("📲 Testar WhatsApp", fontSize = 11.sp, color = Color(0xFF25D366), fontWeight = FontWeight.Bold)
                                    }

                                    TextButton(
                                        onClick = {
                                            val clipText = clipboardManager.getText()?.text
                                            if (!clipText.isNullOrBlank()) {
                                                parseAndApplySharedContent(clipText)
                                            } else {
                                                Toast.makeText(context, "Nenhum link encontrado na área de transferência.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp), tint = vibrantBlue)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Colar Link", fontSize = 11.sp, color = vibrantBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = mapsUrl,
                                onValueChange = {
                                    mapsUrl = it
                                    if (it.contains("http") || it.contains("maps") || it.contains("goo.gl")) {
                                        processLocationExtraction(it)
                                    }
                                },
                                label = { Text("Link de Compartilhamento (maps.app.goo.gl/...)", color = textGray) },
                                placeholder = { Text("Cole o link ex: https://maps.app.goo.gl/...", color = textGray.copy(0.4f)) },
                                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = vibrantBlue) },
                                trailingIcon = {
                                    if (isExtractingLocation) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = vibrantBlue,
                                            strokeWidth = 2.dp
                                        )
                                    } else if (mapsUrl.isNotEmpty()) {
                                        IconButton(onClick = { processLocationExtraction(mapsUrl) }) {
                                            Icon(Icons.Default.AutoFixHigh, contentDescription = "Extrair", tint = vibrantBlue)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = cardBg,
                                    unfocusedContainerColor = cardBg,
                                    focusedTextColor = textWhite,
                                    unfocusedTextColor = textWhite,
                                    focusedBorderColor = vibrantBlue,
                                    unfocusedBorderColor = borderGray
                                )
                            )

                            Button(
                                onClick = { processLocationExtraction(mapsUrl) },
                                enabled = mapsUrl.isNotBlank() && !isExtractingLocation,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937), contentColor = vibrantBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Icon(Icons.Default.PinDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isExtractingLocation) "Expandindo Link & Extraindo GPS..." else "Extrair Posição e Marcar no Mapa",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (extractedFeedback != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (extractedFeedback!!.startsWith("✅")) Color(0xFF2E7D32).copy(0.2f) else Color(0xFFE53935).copy(0.2f),
                                    border = BorderStroke(1.dp, if (extractedFeedback!!.startsWith("✅")) PoolGreen else PoolRed)
                                ) {
                                    Text(
                                        text = extractedFeedback!!,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (extractedFeedback!!.startsWith("✅")) PoolGreen else PoolRed,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Address Text Field
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it; showError = false },
                        label = { Text("Endereço Completo / Referência *", color = textGray) },
                        placeholder = { Text("Ex: Av. Bernardo Sayão, Inhumas - GO", color = textGray.copy(0.4f)) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = vibrantBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        isError = showError && address.isBlank(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = darkBg,
                            unfocusedContainerColor = darkBg,
                            focusedTextColor = textWhite,
                            unfocusedTextColor = textWhite,
                            focusedBorderColor = vibrantBlue,
                            unfocusedBorderColor = borderGray
                        )
                    )

                    // Coordinates display/edit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = latitude.toString(),
                            onValueChange = { it.toDoubleOrNull()?.let { v -> latitude = v } },
                            label = { Text("Latitude GPS", color = textGray) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = darkBg,
                                unfocusedContainerColor = darkBg,
                                focusedTextColor = textWhite,
                                unfocusedTextColor = textWhite,
                                focusedBorderColor = vibrantBlue,
                                unfocusedBorderColor = borderGray
                            )
                        )

                        OutlinedTextField(
                            value = longitude.toString(),
                            onValueChange = { it.toDoubleOrNull()?.let { v -> longitude = v } },
                            label = { Text("Longitude GPS", color = textGray) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = darkBg,
                                unfocusedContainerColor = darkBg,
                                focusedTextColor = textWhite,
                                unfocusedTextColor = textWhite,
                                focusedBorderColor = vibrantBlue,
                                unfocusedBorderColor = borderGray
                            )
                        )
                    }
                }
            }

            // Pool & Service Specifications Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderGray)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "🏊 Especificações Técnicas da Piscina",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textWhite
                    )

                    // Service Type Preset Options
                    OutlinedTextField(
                        value = serviceType,
                        onValueChange = { serviceType = it },
                        label = { Text("Tipo de Serviço Frequente", color = textGray) },
                        leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = vibrantBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = darkBg,
                            unfocusedContainerColor = darkBg,
                            focusedTextColor = textWhite,
                            unfocusedTextColor = textWhite,
                            focusedBorderColor = vibrantBlue,
                            unfocusedBorderColor = borderGray
                        )
                    )

                    val servicePresets = listOf(
                        "Limpeza Semanal de Rotina",
                        "Tratamento de Choque",
                        "Aspiração & Medição",
                        "Manutenção Clorador"
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(servicePresets) { preset ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (serviceType == preset) vibrantBlue.copy(0.2f) else Color(0xFF21262D),
                                border = BorderStroke(1.dp, if (serviceType == preset) vibrantBlue else borderGray),
                                modifier = Modifier.clickable { serviceType = preset }
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 11.sp,
                                    color = if (serviceType == preset) vibrantBlue else textGray,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Liters and time slot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = poolSizeStr,
                            onValueChange = { poolSizeStr = it },
                            label = { Text("Volume (Litros)", color = textGray) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = darkBg,
                                unfocusedContainerColor = darkBg,
                                focusedTextColor = textWhite,
                                unfocusedTextColor = textWhite,
                                focusedBorderColor = vibrantBlue,
                                unfocusedBorderColor = borderGray
                            )
                        )

                        OutlinedTextField(
                            value = timeSlot,
                            onValueChange = { timeSlot = it },
                            label = { Text("Horário Preferencial", color = textGray) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = darkBg,
                                unfocusedContainerColor = darkBg,
                                focusedTextColor = textWhite,
                                unfocusedTextColor = textWhite,
                                focusedBorderColor = vibrantBlue,
                                unfocusedBorderColor = borderGray
                            )
                        )
                    }

                    // Volume quick chips
                    val volumePresets = listOf("35000", "50000", "85000", "120000")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(volumePresets) { vol ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (poolSizeStr == vol) vibrantBlue.copy(0.2f) else Color(0xFF21262D),
                                border = BorderStroke(1.dp, if (poolSizeStr == vol) vibrantBlue else borderGray),
                                modifier = Modifier.clickable { poolSizeStr = vol }
                            ) {
                                Text(
                                    text = "%,d L".format(vol.toInt()),
                                    fontSize = 11.sp,
                                    color = if (poolSizeStr == vol) vibrantBlue else textGray,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = phLevelStr,
                            onValueChange = { phLevelStr = it },
                            label = { Text("pH Ideal (7.2-7.6)", color = textGray) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = darkBg,
                                unfocusedContainerColor = darkBg,
                                focusedTextColor = textWhite,
                                unfocusedTextColor = textWhite,
                                focusedBorderColor = vibrantBlue,
                                unfocusedBorderColor = borderGray
                            )
                        )

                        OutlinedTextField(
                            value = chlorineStr,
                            onValueChange = { chlorineStr = it },
                            label = { Text("Cloro ppm (2 - 4)", color = textGray) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = darkBg,
                                unfocusedContainerColor = darkBg,
                                focusedTextColor = textWhite,
                                unfocusedTextColor = textWhite,
                                focusedBorderColor = vibrantBlue,
                                unfocusedBorderColor = borderGray
                            )
                        )
                    }

                    OutlinedTextField(
                        value = technicianName,
                        onValueChange = { technicianName = it },
                        label = { Text("Piscineiro / Técnico Responsável", color = textGray) },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = vibrantBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = darkBg,
                            unfocusedContainerColor = darkBg,
                            focusedTextColor = textWhite,
                            unfocusedTextColor = textWhite,
                            focusedBorderColor = vibrantBlue,
                            unfocusedBorderColor = borderGray
                        )
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Observações da Casa de Máquinas / Equipamentos", color = textGray) },
                        placeholder = { Text("Ex: Filtro com bomba de 1.5CV, dosador de cloro flutuante...", color = textGray.copy(0.4f)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = darkBg,
                            unfocusedContainerColor = darkBg,
                            focusedTextColor = textWhite,
                            unfocusedTextColor = textWhite,
                            focusedBorderColor = vibrantBlue,
                            unfocusedBorderColor = borderGray
                        )
                    )

                    // Photo of Location / Pool Facade Input
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "📸 Foto da Fachada do Local / Piscina (Opcional)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textWhite
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(darkBg)
                                .clickable { galleryLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUri.isNotBlank()) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = "Foto da Fachada",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Toque para anexar foto da fachada ou piscina", fontSize = 12.sp, color = textGray)
                                }
                            }
                        }
                    }
                }
            }

            if (showError) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEF5350).copy(0.15f),
                    border = BorderStroke(1.dp, Color(0xFFEF5350)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ Preencha o Nome do Cliente e o Endereço/Link do Google Maps para salvar.",
                        color = Color(0xFFFF8A80),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (clientName.isBlank() || address.isBlank()) {
                        showError = true
                    } else {
                        val poolLiters = poolSizeStr.toIntOrNull() ?: 50000
                        val ph = phLevelStr.toFloatOrNull() ?: 7.2f
                        val chlorine = chlorineStr.toFloatOrNull() ?: 2.0f

                        val newTask = PoolTaskEntity(
                            clientName = clientName.trim(),
                            address = address.trim(),
                            poolSizeLiters = poolLiters,
                            serviceType = serviceType.trim(),
                            date = "2026-08-16",
                            timeSlot = timeSlot.trim(),
                            status = status,
                            phLevel = ph,
                            chlorineLevel = chlorine,
                            notes = notes.trim(),
                            technicianName = technicianName.trim(),
                            latitude = latitude,
                            longitude = longitude,
                            photoUri = photoUri,
                            isCompleted = (status == "VERDE"),
                            waterState = if (status == "VERDE") "TRATADA" else "TRATAMENTO_PENDENTE",
                            cleaningState = if (status == "VERDE") "CONCLUIDA" else "PENDENTE",
                            historyLog = "📅 Cadastro do cliente e piscina realizado no sistema."
                        )
                        viewModel.insertTask(newTask)
                        Toast.makeText(context, "Cliente salvo com sucesso e plotado no Mapa!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = vibrantBlue, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salvar Cliente & Plotar no Mapa", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DarkStatusColorOption(
    color: Color,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) color.copy(alpha = 0.22f) else Color(0xFF0D1117),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) color else Color(0xFF30363D)),
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF8B949E),
                maxLines = 1
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = if (isSelected) color else Color(0xFF8B949E),
                maxLines = 1
            )
        }
    }
}
