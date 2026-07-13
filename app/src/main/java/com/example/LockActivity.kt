package com.example

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class LockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_LOCKED_PACKAGE = "extra_locked_package"
    }

    private var targetPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        targetPackage = intent.getStringExtra(EXTRA_LOCKED_PACKAGE)

        // Handle Back Press securely: Redirect to Home Screen to prevent bypass
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                redirectToHome()
            }
        })

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121214) // Deep TV Cinema background
                ) {
                    PinLockScreen(
                        targetPackage = targetPackage ?: "com.google.android.youtube",
                        onSuccess = {
                            AppLockManager.setSessionUnlocked(targetPackage)
                            finish()
                        },
                        onCancel = {
                            redirectToHome()
                        }
                    )
                }
            }
        }
    }

    private fun redirectToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

@Composable
fun PinLockScreen(
    targetPackage: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val cleanAppName = remember(targetPackage) {
        when {
            targetPackage.contains("youtube.kids") -> "YouTube Kids"
            targetPackage.contains("youtube") -> "YouTube"
            else -> targetPackage.substringAfterLast(".")
        }
    }

    // Capture physical remote key presses or emulator keyboard inputs directly on the layout
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val handleDigitInput = { digit: String ->
        if (pinInput.length < 4) {
            isError = false
            pinInput += digit
            if (pinInput.length == 4) {
                // Verify PIN
                val correctPin = AppLockManager.getPin(context)
                if (pinInput == correctPin) {
                    onSuccess()
                } else {
                    isError = true
                    errorMessage = "Hatalı PIN! Lütfen tekrar deneyin."
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
                    } else if (virtualKey == AndroidKeyEvent.KEYCODE_BACK) {
                        onCancel()
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
            // Header Section
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked App Icon",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 12.dp)
            )

            Text(
                text = "$cleanAppName Kilitli",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Text(
                text = "Lütfen erişmek için 4 haneli PIN kodunu girin.",
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

            // TV Remote Optimized PIN Grid
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
                            TVKeyButton(
                                text = digit,
                                onClick = { handleDigitInput(digit) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Backspace Button
                    TVIconButton(
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
                    TVKeyButton(
                        text = "0",
                        onClick = { handleDigitInput("0") }
                    )

                    // Cancel / Home Button
                    TVIconButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = Color.White
                            )
                        },
                        onClick = onCancel
                    )
                }
            }
        }
    }
}

@Composable
fun TVKeyButton(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Smooth TV Remote selection animation
    val scale by animateFloatAsState(if (isFocused) 1.15f else 1.0f, label = "buttonScale")
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
fun TVIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Smooth TV Remote selection animation
    val scale by animateFloatAsState(if (isFocused) 1.15f else 1.0f, label = "iconButtonScale")
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
