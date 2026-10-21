package com.yogesh.multiaichat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.RichText
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.TableStyle
import com.halilibo.richtext.ui.material3.Material3RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

// Light Theme Tokens
private val AppDarkGreen = Color(0xFF14201A)
private val AppDarkGreenSurface = Color(0xFF1B2620)
private val CanvasLight = Color(0xFFF9F9F6)
private val BrandGreen = Color(0xFF3F5E4D)
private val UserBubble = Color(0xFFECEBE4)
private val InputBgLight = Color(0xFFFFFFFF)
private val InputBorderLight = Color(0xFFE2E4DE)
private val SidebarText = Color(0xFFE6EDE8)
private val SidebarMuted = Color(0xFF8A9A90)
private val BodyTextLight = Color(0xFF1F2923)
private val CaptionText = Color(0xFF7C8881)

// Dark Theme Tokens
private val CanvasDark = Color(0xFF121815)
private val BodyTextDark = Color(0xFFE6EDE8)
private val InputBgDark = Color(0xFF1E2822)
private val InputBorderDark = Color(0xFF2C3A32)
private val UserBubbleDark = Color(0xFF28362D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChatApi.configure(this)
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(false) }
            AsterAiTheme(isDark = isDarkTheme) {
                AsterAiApp(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}

@Composable
private fun AsterAiTheme(isDark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = BrandGreen,
            onPrimary = Color.White,
            secondary = BrandGreen,
            background = if (isDark) CanvasDark else CanvasLight,
            surface = if (isDark) InputBgDark else Color.White,
            onSurface = if (isDark) BodyTextDark else BodyTextLight,
            onBackground = if (isDark) BodyTextDark else BodyTextLight
        ),
        content = content
    )
}

@Composable
private fun AsterAiApp(isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("aster_ai_session", android.content.Context.MODE_PRIVATE)
    }
    var token by rememberSaveable { mutableStateOf(preferences.getString("token", "").orEmpty()) }
    var email by rememberSaveable { mutableStateOf(preferences.getString("email", "").orEmpty()) }
    var name by rememberSaveable { mutableStateOf(preferences.getString("name", "").orEmpty()) }
    var mobileNumber by rememberSaveable { mutableStateOf(preferences.getString("mobileNumber", "").orEmpty()) }
    var photoUrl by rememberSaveable { mutableStateOf(preferences.getString("photoUrl", "").orEmpty()) }

    if (token.isBlank()) {
        AuthScreen(
            isDark = isDarkTheme,
            onAuthenticated = { result ->
                preferences.edit()
                    .putString("token", result.token)
                    .putString("email", result.email)
                    .putString("name", result.name)
                    .putString("mobileNumber", result.mobileNumber)
                    .putString("photoUrl", result.photoUrl)
                    .apply()
                token = result.token
                email = result.email
                name = result.name
                mobileNumber = result.mobileNumber
                photoUrl = result.photoUrl
            }
        )
    } else {
        ChatScreen(
            token = token,
            email = email,
            name = name,
            mobileNumber = mobileNumber,
            photoUrl = photoUrl,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onUpdateProfile = { updated ->
                preferences.edit()
                    .putString("name", updated.name)
                    .putString("mobileNumber", updated.mobileNumber)
                    .putString("photoUrl", updated.photoUrl)
                    .apply()
                name = updated.name
                mobileNumber = updated.mobileNumber
                photoUrl = updated.photoUrl
            },
            onSignOut = {
                preferences.edit().clear().apply()
                token = ""
                email = ""
                name = ""
                mobileNumber = ""
                photoUrl = ""
            }
        )
    }
}

@Composable
private fun ProfileAvatar(
    photoUrl: String,
    name: String,
    modifier: Modifier = Modifier,
    textSize: androidx.compose.ui.unit.TextUnit = 16.sp
) {
    val decodedBitmap = remember(photoUrl) {
        if (photoUrl.contains("base64,")) {
            val base64Data = photoUrl.substringAfter("base64,")
            runCatching {
                val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        } else if (photoUrl.startsWith("http") || photoUrl.startsWith("content") || photoUrl.startsWith("file")) {
            null
        } else if (photoUrl.length > 50) {
            runCatching {
                val bytes = android.util.Base64.decode(photoUrl, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        } else {
            null
        }
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = BrandGreen
    ) {
        if (decodedBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = decodedBitmap,
                contentDescription = "Profile Photo",
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Profile Photo",
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    name.takeIf { it.isNotBlank() }?.take(1)?.uppercase() ?: "U",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = textSize
                )
            }
        }
    }
}

@Composable
private fun BrandMark() {
    Surface(
        modifier = Modifier.size(58.dp),
        shape = CircleShape,
        color = BrandGreen
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(painter = painterResource(R.drawable.aster_logo), contentDescription = null, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)))
        }
    }
}

@Composable
private fun AuthScreen(isDark: Boolean, onAuthenticated: (AuthResult) -> Unit) {
    var createAccount by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var mobileNumber by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val bodyColor = if (isDark) BodyTextDark else BodyTextLight

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) CanvasDark else CanvasLight)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 440.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandMark()
            Spacer(Modifier.height(18.dp))
            Text("aster", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = bodyColor, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(6.dp))
            Text(
                "A little space to think.",
                color = CaptionText,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) InputBgDark else Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        if (createAccount) "Create your account" else "Welcome back",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = bodyColor
                    )
                    if (createAccount) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; error = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Full Name") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen)
                        )
                        OutlinedTextField(
                            value = mobileNumber,
                            onValueChange = { mobileNumber = it; error = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Mobile Number (Optional)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen)
                        )
                    }
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen)
                    )
                    error?.let { InlineError(it) }
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank() || (createAccount && name.isBlank())) {
                                error = "Please fill in required fields."
                                return@Button
                            }
                            busy = true
                            error = null
                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        if (createAccount) ChatApi.register(name.trim(), email.trim(), password, mobileNumber.trim())
                                        else ChatApi.login(email.trim(), password)
                                    }
                                    onAuthenticated(result)
                                } catch (exception: Exception) {
                                    error = exception.message ?: "Could not sign in."
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !busy,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = Color.White)
                    ) {
                        if (busy) CircularProgressIndicator(modifier = Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text(if (createAccount) "Create account" else "Sign in", fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = { createAccount = !createAccount; error = null },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(if (createAccount) "Already have an account? Sign in" else "New here? Create an account", color = BrandGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(
    token: String,
    email: String,
    name: String,
    mobileNumber: String,
    photoUrl: String,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onUpdateProfile: (UserProfile) -> Unit,
    onSignOut: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("chat") } // "chat", "settings", "personal_info"
    var chats by remember { mutableStateOf(emptyList<ChatItem>()) }
    var activeChatId by rememberSaveable { mutableStateOf("") }
    var messages by remember { mutableStateOf(emptyList<ChatMessage>()) }
    var provider by rememberSaveable { mutableStateOf("openai") }
    var providerExpanded by remember { mutableStateOf(false) }
    var prompt by rememberSaveable { mutableStateOf("") }
    var loadingChats by remember { mutableStateOf(true) }
    var loadingMessages by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var isTtsEnabled by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    var ttsInstance: TextToSpeech? by remember { mutableStateOf(null) }
    DisposableEffect(context) {
        val createdTts = TextToSpeech(context, null)
        createdTts.language = Locale.US
        ttsInstance = createdTts
        onDispose {
            createdTts.stop()
            createdTts.shutdown()
        }
    }

    var attachedFile by remember { mutableStateOf<AttachedFile?>(null) }

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            attachedFile = uri.readAttachedFile(context)
            error = null
        }
    }

    val speechLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                prompt = spokenText
            }
        }
    }

    val bodyColor = if (isDarkTheme) BodyTextDark else BodyTextLight
    val canvasBg = if (isDarkTheme) CanvasDark else CanvasLight

    LaunchedEffect(token) {
        try {
            chats = withContext(Dispatchers.IO) { ChatApi.listChats(token) }
            activeChatId = chats.firstOrNull()?.id.orEmpty()
        } catch (exception: Exception) {
            error = exception.message
        } finally {
            loadingChats = false
        }
    }

    LaunchedEffect(token, activeChatId) {
        if (activeChatId.isBlank()) {
            messages = emptyList()
            return@LaunchedEffect
        }
        loadingMessages = true
        try {
            val selected = withContext(Dispatchers.IO) { ChatApi.listMessages(token, activeChatId) }
            messages = selected
        } catch (exception: Exception) {
            error = exception.message
        } finally {
            loadingMessages = false
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    if (currentScreen == "settings") {
        SettingsScreen(
            name = name,
            email = email,
            photoUrl = photoUrl,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onOpenPersonalInfo = { currentScreen = "personal_info" },
            onClose = { currentScreen = "chat" },
            onSignOut = onSignOut
        )
        return
    }

    if (currentScreen == "personal_info") {
        PersonalInfoScreen(
            token = token,
            name = name,
            email = email,
            mobileNumber = mobileNumber,
            photoUrl = photoUrl,
            isDark = isDarkTheme,
            onClose = { currentScreen = "settings" },
            onSaved = { updated ->
                onUpdateProfile(updated)
                currentScreen = "settings"
            }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = AppDarkGreen,
                modifier = Modifier.width(300.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(painter = painterResource(R.drawable.aster_logo), contentDescription = null, modifier = Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)))
                        Spacer(Modifier.width(12.dp))
                        Text("aster", fontWeight = FontWeight.Medium, fontSize = 22.sp, color = SidebarText, fontFamily = FontFamily.Serif)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Text("✕", color = SidebarText, fontSize = 18.sp)
                        }
                    }
                    Button(
                        onClick = {
                            error = null
                            scope.launch {
                                drawerState.close()
                                try {
                                    val created = withContext(Dispatchers.IO) { ChatApi.createChat(token, provider) }
                                    chats = listOf(created) + chats
                                    activeChatId = created.id
                                    messages = emptyList()
                                } catch (exception: Exception) { error = exception.message }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = AppDarkGreenSurface, contentColor = SidebarText)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("+ New chat", fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    Text("RECENT", modifier = Modifier.padding(horizontal = 24.dp), color = SidebarMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        items(chats, key = { it.id }) { chat ->
                            val isActive = chat.id == activeChatId
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                onClick = { activeChatId = chat.id; error = null; scope.launch { drawerState.close() } },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isActive) AppDarkGreenSurface else Color.Transparent
                            ) {
                                Text(
                                    chat.title,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    color = if (isActive) SidebarText else SidebarMuted,
                                    maxLines = 1,
                                    fontSize = 14.sp,
                                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Card(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AppDarkGreenSurface)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(painter = painterResource(R.drawable.aster_logo), contentDescription = null, modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)))
                            Spacer(Modifier.width(12.dp))
                            Text("A little space to think. Ask anything, get somewhere.", color = SidebarText, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                    HorizontalDivider(color = Color(0x1AFFFFFF))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { scope.launch { drawerState.close() }; currentScreen = "settings" }.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileAvatar(
                            photoUrl = photoUrl,
                            name = name,
                            modifier = Modifier.size(36.dp),
                            textSize = 16.sp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name.ifBlank { "User" }, fontWeight = FontWeight.Medium, color = SidebarText, fontSize = 14.sp)
                            Text(email, color = SidebarMuted, fontSize = 12.sp)
                        }
                        Text("⟩", color = SidebarMuted, fontSize = 16.sp)
                    }
                }
            }
        }
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = canvasBg) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
                // Top App Bar
                Row(
                    modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Text("☰", fontSize = 24.sp, color = bodyColor)
                    }
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) InputBorderDark else InputBorderLight),
                            modifier = Modifier.padding(start = 4.dp).clickable { providerExpanded = true }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(BrandGreen, CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Text(if (provider == "openai") "Fastest response" else "Deep thinking", fontSize = 13.sp, color = bodyColor, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.width(6.dp))
                                Text("▾", fontSize = 12.sp, color = bodyColor)
                            }
                        }
                        DropdownMenu(
                            expanded = providerExpanded,
                            onDismissRequest = { providerExpanded = false },
                            modifier = Modifier.background(if (isDarkTheme) InputBgDark else InputBgLight)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Fastest response", color = bodyColor) },
                                onClick = { provider = "openai"; providerExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Deep thinking", color = bodyColor) },
                                onClick = { provider = "gemini"; providerExpanded = false }
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {
                        isTtsEnabled = !isTtsEnabled
                        if (!isTtsEnabled) ttsInstance?.stop()
                    }) {
                        Text(if (isTtsEnabled) "🔊" else "🔇", fontSize = 18.sp, color = bodyColor)
                    }
                    IconButton(onClick = { currentScreen = "settings" }) {
                        Text("⚙\uFE0F", fontSize = 20.sp, color = bodyColor)
                    }
                }

                if (messages.isEmpty() && !loadingMessages) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) InputBorderDark else InputBorderLight)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(painter = painterResource(R.drawable.aster_logo), contentDescription = null, modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)))
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "What can I help you with?",
                            color = bodyColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Serif,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (loadingMessages && messages.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandGreen)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                isDark = isDarkTheme,
                                onSpeak = { text ->
                                    ttsInstance?.speak(text.take(500), TextToSpeech.QUEUE_FLUSH, null, "aster_message")
                                }
                            )
                        }
                        if (sending) {
                            item(key = "typing-indicator") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Surface(
                                        modifier = Modifier.size(28.dp),
                                        shape = CircleShape,
                                        color = Color.Transparent,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) InputBorderDark else InputBorderLight)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Image(painter = painterResource(R.drawable.aster_logo), contentDescription = null, modifier = Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)))
                                        }
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.padding(top = 4.dp)) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = BrandGreen,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                error?.let {
                    InlineError(
                        it,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Input Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (isDarkTheme) InputBgDark else InputBgLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) InputBorderDark else InputBorderLight),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            attachedFile?.let { file ->
                                Surface(
                                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp).fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDarkTheme) Color(0xFF233228) else Color(0xFFEBF1ED),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreen)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("📁", fontSize = 16.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(file.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = bodyColor, maxLines = 1)
                                            Text(file.sizeString, fontSize = 11.sp, color = CaptionText)
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { attachedFile = null },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Text("✕", fontSize = 12.sp, color = CaptionText)
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = prompt,
                                onValueChange = { prompt = it; error = null },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp, max = 120.dp),
                                placeholder = { Text("Message aster...", color = CaptionText) },
                                maxLines = 5,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = bodyColor,
                                    unfocusedTextColor = bodyColor
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isDarkTheme) Color(0xFF233228) else Color(0xFFEBF1ED),
                                        modifier = Modifier.clickable { runCatching { filePickerLauncher.launch("*/*") } }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("📎", fontSize = 14.sp)
                                            Spacer(Modifier.width(4.dp))
                                            Text("File", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = bodyColor)
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isDarkTheme) Color(0xFF233228) else Color(0xFFEBF1ED),
                                        modifier = Modifier.clickable {
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Aster...")
                                            }
                                            runCatching { speechLauncher.launch(intent) }.onFailure {
                                                error = "Speech recognition is not available on this device."
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🎙️", fontSize = 14.sp)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Voice", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = bodyColor)
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(painter = painterResource(R.drawable.aster_logo), contentDescription = null, modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Aster AI", color = CaptionText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            HorizontalDivider(color = if (isDarkTheme) InputBorderDark.copy(alpha = 0.5f) else InputBorderLight.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val canSend = (prompt.isNotBlank() || attachedFile != null) && !sending
                                val buttonScale by animateFloatAsState(
                                    targetValue = if (canSend) 1.02f else 0.96f,
                                    animationSpec = tween(200),
                                    label = "sendScale"
                                )
                                val buttonColor by animateColorAsState(
                                    targetValue = if (canSend) BrandGreen else if (isDarkTheme) Color(0xFF2C3A32) else Color(0xFFD4DAD5),
                                    animationSpec = tween(200),
                                    label = "sendColor"
                                )
                                val infiniteTransition = rememberInfiniteTransition(label = "sendingSpinner")
                                val spinnerAngle by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(tween(750)),
                                    label = "spinnerAngle"
                                )

                                Surface(
                                    onClick = {
                                        var chatId = activeChatId
                                        val file = attachedFile
                                        val rawText = prompt.trim()
                                        if ((rawText.isBlank() && file == null) || sending) return@Surface

                                        val queryTitle = if (rawText.isNotBlank()) rawText.take(30).replace("\n", " ").trim() else file?.name?.take(30) ?: "New chat"

                                        val messageToSend = buildString {
                                            if (file != null) {
                                                append("[Attached File: ${file.name} (${file.sizeString})]\n")
                                                if (!file.contentText.isNullOrBlank()) {
                                                    append("File Content:\n```\n${file.contentText}\n```\n\n")
                                                }
                                            }
                                            if (rawText.isNotBlank()) {
                                                append(rawText)
                                            }
                                        }

                                        prompt = ""
                                        attachedFile = null
                                        sending = true
                                        error = null
                                        scope.launch {
                                            try {
                                                if (chatId.isBlank()) {
                                                    val created = withContext(Dispatchers.IO) { ChatApi.createChat(token, provider) }
                                                    chats = listOf(created.copy(title = queryTitle)) + chats
                                                    chatId = created.id
                                                    activeChatId = chatId
                                                } else {
                                                    chats = chats.map { if (it.id == chatId) it.copy(title = queryTitle) else it }
                                                }
                                                val optimistic = ChatMessage("local-${UUID.randomUUID()}", chatId, "user", messageToSend, provider, System.currentTimeMillis())
                                                messages = messages + optimistic
                                                val result = withContext(Dispatchers.IO) { ChatApi.sendMessage(token, chatId, provider, messageToSend) }
                                                messages = messages.dropLast(1) + result.userMessage + result.assistantMessage
                                                chats = withContext(Dispatchers.IO) { ChatApi.listChats(token) }

                                                if (isTtsEnabled && result.assistantMessage.content.isNotBlank()) {
                                                    ttsInstance?.speak(result.assistantMessage.content.take(500), TextToSpeech.QUEUE_FLUSH, null, "aster_voice")
                                                }
                                            } catch (exception: Exception) {
                                                if (chatId.isNotBlank()) {
                                                    try {
                                                        withContext(Dispatchers.IO) { messages = ChatApi.listMessages(token, chatId) }
                                                    } catch (_: Exception) {}
                                                }
                                                error = exception.message ?: "Aster server had a problem."
                                            } finally {
                                                sending = false
                                            }
                                        }
                                    },
                                    enabled = canSend,
                                    shape = RoundedCornerShape(20.dp),
                                    color = buttonColor,
                                    shadowElevation = if (canSend) 3.dp else 0.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .scale(buttonScale)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (sending) {
                                            Text("↻", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.rotate(spinnerAngle))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Sending...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        } else {
                                            Text("✈\uFE0F", fontSize = 18.sp)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Send Message", color = if (canSend) Color.White else CaptionText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Aster can make mistakes. Verify important info.", color = CaptionText, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isDark: Boolean,
    onSpeak: ((String) -> Unit)? = null
) {
    val isUser = message.role == "user"
    val bodyColor = if (isDark) BodyTextDark else BodyTextLight
    val bubbleColor = if (isUser) (if (isDark) UserBubbleDark else UserBubble) else Color.Transparent

    if (isUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 300.dp),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp),
                color = bubbleColor,
            ) {
                Text(
                    message.content,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    color = bodyColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                modifier = Modifier.size(28.dp).padding(top = 2.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) InputBorderDark else InputBorderLight)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(painter = painterResource(R.drawable.aster_logo), contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("aster", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = bodyColor, fontFamily = FontFamily.Serif)
                    if (onSpeak != null) {
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { onSpeak(message.content) },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Text("🔊", fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
                    Material3RichText(
                        style = RichTextStyle(
                            stringStyle = RichTextStringStyle(
                                linkStyle = androidx.compose.ui.text.SpanStyle(color = BrandGreen)
                            ),
                            tableStyle = TableStyle(
                                borderColor = if (isDark) Color(0xFF4A5D51) else Color(0xFFE2E8E4),
                                borderStrokeWidth = 2f,
                                cellPadding = 18.sp
                            )
                        )
                    ) {
                        CompositionLocalProvider(
                            LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = bodyColor
                            )
                        ) {
                            Markdown(message.content)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineError(message: String, modifier: Modifier = Modifier) {
    Text(
        message,
        modifier = modifier
            .background(Color(0xFFFFF0EF), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        color = Color(0xFF9B312C),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun SettingsScreen(
    name: String,
    email: String,
    photoUrl: String,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenPersonalInfo: () -> Unit,
    onClose: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var selectedLanguage by rememberSaveable { mutableStateOf("English") }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf<String?>(null) }

    val bodyColor = if (isDarkTheme) BodyTextDark else BodyTextLight
    val canvasBg = if (isDarkTheme) CanvasDark else CanvasLight

    Surface(modifier = Modifier.fillMaxSize(), color = canvasBg) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Text("←", fontSize = 24.sp, color = bodyColor)
                }
                Text(
                    "Settings",
                    modifier = Modifier.weight(1f).padding(end = 48.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = bodyColor
                )
            }
            HorizontalDivider(color = if (isDarkTheme) InputBorderDark else InputBorderLight)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Profile & Account Summary
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ProfileAvatar(
                            photoUrl = photoUrl,
                            name = name,
                            modifier = Modifier.size(80.dp),
                            textSize = 32.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(name.ifBlank { "User" }, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = bodyColor)
                        Text(email, color = CaptionText, fontSize = 13.sp)
                    }
                }

                // Account / Personal Info Option
                item {
                    Text("ACCOUNT", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = SidebarMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    SettingsRow(
                        icon = "👤",
                        title = "Personal Information",
                        subtitle = "Name, Password, Mobile, Avatar URL (Saved in Atlas)",
                        isDark = isDarkTheme,
                        onClick = onOpenPersonalInfo
                    )
                }

                // Preferences Options
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("PREFERENCES", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = SidebarMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    
                    // Dark / Light Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleTheme).padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isDarkTheme) "🌙" else "☀️", fontSize = 20.sp)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Theme Mode", fontSize = 16.sp, color = bodyColor)
                            Text(if (isDarkTheme) "Dark theme enabled" else "Light theme enabled", color = CaptionText, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleTheme() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandGreen)
                        )
                    }

                    // Language Switcher
                    Box {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { languageMenuExpanded = true }.padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌐", fontSize = 20.sp)
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Language", fontSize = 16.sp, color = bodyColor)
                                Text(selectedLanguage, color = CaptionText, fontSize = 12.sp)
                            }
                            Text("▾", fontSize = 16.sp, color = CaptionText)
                        }
                        DropdownMenu(
                            expanded = languageMenuExpanded,
                            onDismissRequest = { languageMenuExpanded = false }
                        ) {
                            listOf("English", "Spanish", "Hindi", "French", "German").forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang) },
                                    onClick = {
                                        selectedLanguage = lang
                                        languageMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // External Website Links
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("WEBSITES & LINKS", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = SidebarMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    SettingsRow(
                        icon = "💻",
                        title = "Developer Portfolio",
                        subtitle = "yogesh-portfolio2-psi.vercel.app",
                        isDark = isDarkTheme,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://yogesh-portfolio2-psi.vercel.app/"))
                            context.startActivity(intent)
                        }
                    )
                    SettingsRow(
                        icon = "🚀",
                        title = "Aster AI Web App",
                        subtitle = "ai-chatbot-sigma-rust.vercel.app",
                        isDark = isDarkTheme,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ai-chatbot-sigma-rust.vercel.app/auth.html"))
                            context.startActivity(intent)
                        }
                    )
                }

                // About Options
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("ABOUT", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = SidebarMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    SettingsRow(icon = "📄", title = "Privacy Policy", isDark = isDarkTheme) { showInfoDialog = "Privacy Policy" }
                    SettingsRow(icon = "📜", title = "Terms of Service", isDark = isDarkTheme) { showInfoDialog = "Terms of Service" }
                    SettingsRow(icon = "✉️", title = "Contact Us", isDark = isDarkTheme) { showInfoDialog = "Contact Us" }
                }

                // Sign Out
                item {
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
                        shape = CircleShape
                    ) {
                        Text("Sign out", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }

            if (showInfoDialog != null) {
                AlertDialog(
                    onDismissRequest = { showInfoDialog = null },
                    title = { Text(showInfoDialog!!, color = bodyColor) },
                    text = {
                        Text(
                            when (showInfoDialog) {
                                "Privacy Policy" -> "Aster AI values your privacy. We do not sell your personal data. All conversations and profile details are stored securely in MongoDB Atlas."
                                "Terms of Service" -> "By using Aster AI, you agree to interact respectfully. We reserve the right to suspend accounts violating these terms."
                                "Contact Us" -> "Need help? Email us at support@aster.ai or visit our website."
                                else -> ""
                            },
                            color = bodyColor
                        )
                    },
                    confirmButton = { TextButton(onClick = { showInfoDialog = null }) { Text("Close", color = BrandGreen) } },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = if (isDarkTheme) InputBgDark else Color.White
                )
            }
        }
    }
}

private data class AttachedFile(
    val uri: Uri,
    val name: String,
    val sizeString: String,
    val contentText: String? = null
)

private fun Uri.readAttachedFile(context: android.content.Context): AttachedFile {
    var name = "Attached file"
    var sizeBytes = 0L
    
    runCatching {
        context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
            }
        }
    }

    val sizeKb = if (sizeBytes > 0) "${sizeBytes / 1024} KB" else "File"

    val textContent = runCatching {
        context.contentResolver.openInputStream(this)?.use { stream ->
            val bytes = stream.readBytes()
            if (bytes.size < 500_000) {
                val text = String(bytes, Charsets.UTF_8)
                if (text.none { it < ' ' && it != '\n' && it != '\r' && it != '\t' }) text else null
            } else null
        }
    }.getOrNull()

    return AttachedFile(
        uri = this,
        name = name,
        sizeString = sizeKb,
        contentText = textContent
    )
}

private fun Uri.toBase64Jpeg(context: android.content.Context): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(this) ?: return null
        val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream) ?: return null
        inputStream.close()
        val maxDimension = 300
        val width = originalBitmap.width
        val height = originalBitmap.height
        val (newWidth, newHeight) = if (width > height) {
            maxDimension to (height * maxDimension / width)
        } else {
            (width * maxDimension / height) to maxDimension
        }
        val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        val outputStream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        "data:image/jpeg;base64," + android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun PersonalInfoScreen(
    token: String,
    name: String,
    email: String,
    mobileNumber: String,
    photoUrl: String,
    isDark: Boolean,
    onClose: () -> Unit,
    onSaved: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    var editName by rememberSaveable { mutableStateOf(name) }
    var editMobile by rememberSaveable { mutableStateOf(mobileNumber) }
    var editPhotoUrl by rememberSaveable { mutableStateOf(photoUrl) }
    var editPassword by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val base64 = uri.toBase64Jpeg(context)
            if (base64 != null) {
                editPhotoUrl = base64
                error = null
            } else {
                error = "Could not load selected photo."
            }
        }
    }

    val bodyColor = if (isDark) BodyTextDark else BodyTextLight
    val canvasBg = if (isDark) CanvasDark else CanvasLight

    Surface(modifier = Modifier.fillMaxSize(), color = canvasBg) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Text("←", fontSize = 24.sp, color = bodyColor)
                }
                Text(
                    "Personal Information",
                    modifier = Modifier.weight(1f).padding(end = 48.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = bodyColor
                )
            }
            HorizontalDivider(color = if (isDark) InputBorderDark else InputBorderLight)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clickable {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest.Builder()
                                        .setMediaType(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        .build()
                                )
                            }
                    ) {
                        ProfileAvatar(
                            photoUrl = editPhotoUrl,
                            name = editName,
                            modifier = Modifier.fillMaxSize(),
                            textSize = 36.sp
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).size(28.dp),
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 3.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text("📷", fontSize = 14.sp) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest.Builder()
                                    .setMediaType(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    .build()
                            )
                        }
                    ) {
                        Text("Edit Profile Picture", color = BrandGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it; error = null },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {},
                    enabled = false,
                    label = { Text("Email Address (Immutable)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = editMobile,
                    onValueChange = { editMobile = it; error = null },
                    label = { Text("Mobile Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen)
                )

                OutlinedTextField(
                    value = editPassword,
                    onValueChange = { editPassword = it; error = null },
                    label = { Text("New Password (Optional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text("Leave empty to keep current password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen)
                )

                error?.let { InlineError(it) }
                successMsg?.let {
                    Text(it, color = BrandGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        if (editName.isBlank()) {
                            error = "Name cannot be empty."
                            return@Button
                        }
                        busy = true
                        error = null
                        successMsg = null
                        scope.launch {
                            try {
                                val updated = withContext(Dispatchers.IO) {
                                    ChatApi.updateProfile(
                                        token = token,
                                        name = editName.trim(),
                                        password = editPassword.trim().ifBlank { null },
                                        mobileNumber = editMobile.trim(),
                                        photoUrl = editPhotoUrl.trim()
                                    )
                                }
                                successMsg = "Saved to MongoDB Atlas successfully!"
                                onSaved(updated)
                            } catch (e: Exception) {
                                error = e.message ?: "Failed to save profile."
                            } finally {
                                busy = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !busy,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = Color.White)
                ) {
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Save Changes to Database", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: String,
    title: String,
    subtitle: String? = null,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val bodyColor = if (isDark) BodyTextDark else BodyTextLight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = bodyColor)
            if (subtitle != null) {
                Text(subtitle, color = CaptionText, fontSize = 12.sp)
            }
        }
        Text("⟩", color = CaptionText, fontSize = 16.sp)
    }
}
