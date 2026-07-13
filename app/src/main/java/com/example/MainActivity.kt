package com.example

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.foundation.focusable
import androidx.compose.ui.text.font.FontFamily
import android.view.KeyEvent as AndroidKeyEvent
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121214) // Cinematic Dark background suited for Google TV
                ) {
                    var isDashboardUnlocked by remember { mutableStateOf(false) }

                    if (isDashboardUnlocked) {
                        TVAppLockDashboard()
                    } else {
                        DashboardPinLockScreen(
                            onSuccess = {
                                isDashboardUnlocked = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TVAppLockDashboard() {
    val context = LocalContext.current
    var isAccessibilityGranted by remember { mutableStateOf(false) }
    var isOverlayGranted by remember { mutableStateOf(false) }
    
    // State to force UI refresh when returning from settings
    var refreshTrigger by remember { mutableStateOf(0) }

    // Check permissions periodically or when app is resumed
    LaunchedEffect(refreshTrigger) {
        isAccessibilityGranted = isAccessibilityServiceEnabled(context, AppLockAccessibilityService::class.java)
        isOverlayGranted = Settings.canDrawOverlays(context)
    }

    // Refresh when app is brought back to foreground
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shield",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "TV AppLock Dashboard",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1E),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121214)
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Side: Permissions & Info
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Gerekli İzinler & Durum",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.LightGray,
                    fontWeight = FontWeight.SemiBold
                )

                PermissionCard(
                    title = "Erişilebilirlik Servisi",
                    description = "Uygulamaların açılışını anlık takip edebilmek için bu servisi etkinleştirin.",
                    isGranted = isAccessibilityGranted,
                    icon = Icons.Default.Accessibility,
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                            Toast.makeText(context, "Lütfen listeden 'TV AppLock' servisini aktif edin.", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Ayarlar sayfası açılamadı.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                PermissionCard(
                    title = "Sistem Penceresi Üzerinde Gösterim",
                    description = "Kilit ekranını YouTube üzerinde Overlay olarak güvenle açabilmek için bu izni verin.",
                    isGranted = isOverlayGranted,
                    icon = Icons.Default.SettingsSystemDaydream,
                    onClick = {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback if direct package URI fails on some TV platforms
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                context.startActivity(intent)
                            } catch (err: Exception) {
                                Toast.makeText(context, "Sistem ayarları sayfası açılamadı.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                // TV Instruction Info Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E24), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kumanda / Klavye Desteği",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Kilit ekranımız tamamen Google TV kumandası D-pad yön tuşlarıyla ve fiziksel klavye rakam tuşlarıyla kontrol edilmeye uyumludur.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Right Side: PIN Config & Apps List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // PIN Setup Section
                Text(
                    text = "Güvenlik Ayarları",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.LightGray,
                    fontWeight = FontWeight.SemiBold
                )

                PinConfigCard()

                // Locked Apps list Section
                Text(
                    text = "Korunacak Uygulamalar",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.LightGray,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                LockedAppsList()
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    val scale by animateFloatAsState(if (isFocused) 1.04f else 1.0f, label = "cardScale")
    val borderColor = if (isFocused) MaterialTheme.colorScheme.primary else Color.DarkGray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFF25252B) else Color(0xFF1E1E24)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(36.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Indicator
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "AKTİF ET",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PinConfigCard() {
    val context = LocalContext.current
    var currentPin by remember { mutableStateOf("") }
    var pinEditValue by remember { mutableStateOf("") }
    var showEditField by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        currentPin = AppLockManager.getPin(context)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    val scale by animateFloatAsState(if (isFocused) 1.04f else 1.0f, label = "pinCardScale")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .border(
                2.dp,
                if (isFocused) MaterialTheme.colorScheme.primary else Color.DarkGray,
                RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = { showEditField = !showEditField }
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = "PIN Key",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Aktif PIN Kodu",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = currentPin,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                Button(
                    onClick = { showEditField = !showEditField },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = if (showEditField) "Kapat" else "Değiştir")
                }
            }

            AnimatedVisibility(visible = showEditField) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = pinEditValue,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pinEditValue = it
                            }
                        },
                        label = { Text("Yeni 4 Haneli PIN", color = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (pinEditValue.length == 4) {
                                AppLockManager.savePin(context, pinEditValue)
                                currentPin = pinEditValue
                                pinEditValue = ""
                                showEditField = false
                                Toast.makeText(context, "PIN başarıyla güncellendi!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "PIN tam olarak 4 haneli olmalıdır!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(text = "Yeni PIN'i Kaydet")
                    }
                }
            }
        }
    }
}

data class AppItem(val name: String, val packageName: String, val icon: ImageVector)

@Composable
fun LockedAppsList() {
    val context = LocalContext.current
    var lockedPackages by remember { mutableStateOf(setOf<String>()) }

    // Hardcoded demo applications commonly installed on Android TV
    val appsList = remember {
        listOf(
            AppItem("YouTube", "com.google.android.youtube", Icons.Default.PlayArrow),
            AppItem("YouTube Kids", "com.google.android.youtube.kids", Icons.Default.ChildCare),
            AppItem("Netflix", "com.netflix.mediaclient", Icons.Default.Movie),
            AppItem("Prime Video", "com.amazon.amazonvideo.livingroom", Icons.Default.Tv)
        )
    }

    LaunchedEffect(Unit) {
        lockedPackages = AppLockManager.getLockedPackages(context)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(appsList) { app ->
            val isLocked = lockedPackages.contains(app.packageName)
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            
            val scale by animateFloatAsState(if (isFocused) 1.03f else 1.0f, label = "appCardScale")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isFocused) Color(0xFF25252B) else Color(0xFF1E1E24))
                    .border(
                        1.dp,
                        if (isFocused) MaterialTheme.colorScheme.primary else Color.DarkGray,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = {
                            AppLockManager.togglePackageLock(context, app.packageName)
                            lockedPackages = AppLockManager.getLockedPackages(context)
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = app.icon,
                        contentDescription = null,
                        tint = if (isLocked) MaterialTheme.colorScheme.error else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                Switch(
                    checked = isLocked,
                    onCheckedChange = {
                        AppLockManager.togglePackageLock(context, app.packageName)
                        lockedPackages = AppLockManager.getLockedPackages(context)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}

/**
 * Robust helper function to verify if our Accessibility Service is enabled in settings.
 */
fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<out AccessibilityService>): Boolean {
    val expectedComponentName = ComponentName(context, serviceClass)
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledComponent = ComponentName.unflattenFromString(componentNameString)
        if (enabledComponent != null && enabledComponent == expectedComponentName) {
            return true
        }
    }
    return false
}

@Composable
fun DashboardPinLockScreen(onSuccess: () -> Unit) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val handleDigitInput = { digit: String ->
        if (pinInput.length < 4) {
            isError = false
            pinInput += digit
            if (pinInput.length == 4) {
                if (pinInput == "2650") {
                    onSuccess()
                } else {
                    isError = true
                    errorMessage = "Hatalı Yönetici Şifresi! Lütfen tekrar deneyin."
                    pinInput = ""
                }
            }
        }
    }

    val handleBackspace = {
        if (pinInput.isNotEmpty()) {
            pinInput = pinInput.dropLast(1)
            isError = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val virtualKey = keyEvent.nativeKeyEvent.keyCode
                    if (virtualKey >= AndroidKeyEvent.KEYCODE_0 && virtualKey <= AndroidKeyEvent.KEYCODE_9) {
                        val digit = (virtualKey - AndroidKeyEvent.KEYCODE_0).toString()
                        handleDigitInput(digit)
                        true
                    } else if (virtualKey == AndroidKeyEvent.KEYCODE_DEL) {
                        handleBackspace()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = "Admin Shield",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 12.dp)
            )

            Text(
                text = "Yönetim Paneli Girişi",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Text(
                text = "Yönetici ayarlarına erişmek için lütfen 4 haneli şifreyi girin.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // PIN Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < pinInput.length
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(
                                width = 2.dp,
                                color = if (isError) Color.Red else MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                            .background(
                                color = if (isError) {
                                    Color.Red.copy(alpha = 0.3f)
                                } else if (isFilled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                shape = CircleShape
                            )
                    )
                }
            }

            // Error Display
            AnimatedVisibility(visible = isError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // TV Remote PIN Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )

                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { digit ->
                            DashboardTVKeyButton(
                                text = digit,
                                onClick = { handleDigitInput(digit) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Backspace Button
                    DashboardTVIconButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Clear",
                                tint = Color.White
                            )
                        },
                        onClick = { handleBackspace() }
                    )

                    // 0 Button
                    DashboardTVKeyButton(
                        text = "0",
                        onClick = { handleDigitInput("0") }
                    )

                    // Admin icon placeholder for spacing
                    DashboardTVIconButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Dashboard",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = { }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardTVKeyButton(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(if (isFocused) 1.15f else 1.0f, label = "dashboardButtonScale")
    val backgroundColor = if (isFocused) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color(0xFF232328)
    }

    val contentColor = if (isFocused) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        Color.White
    }

    val borderStroke = if (isFocused) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, Color.DarkGray)
    }

    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(width = 80.dp, height = 56.dp)
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        border = borderStroke
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            fontSize = 22.sp
        )
    }
}

@Composable
fun DashboardTVIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(if (isFocused) 1.15f else 1.0f, label = "dashboardIconButtonScale")
    val backgroundColor = if (isFocused) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color(0xFF232328)
    }

    val borderStroke = if (isFocused) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, Color.DarkGray)
    }

    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(width = 80.dp, height = 56.dp)
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor
        ),
        border = borderStroke,
        contentPadding = PaddingValues(0.dp)
    ) {
        icon()
    }
}
