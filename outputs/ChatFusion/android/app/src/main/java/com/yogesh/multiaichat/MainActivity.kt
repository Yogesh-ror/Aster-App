package com.yogesh.multiaichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.RichText
import com.halilibo.richtext.ui.material3.Material3RichText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private val AppDarkGrey = Color(0xFF1E1E1E)
private val AppLightGrey = Color(0xFF2C2C2C)
private val AppAccentGreen = Color(0xFF5A7D6C)
private val Ink = Color(0xFF201D36)
private val Violet = AppAccentGreen
private val Lavender = Color(0xFFEDEBFF)
private val Canvas = Color(0xFFFFFFFF)
private val Muted = Color(0xFF77748A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChatApi.configure(this)
        setContent {
            AsterAiTheme {
                AsterAiApp()
            }
        }
    }
}

@Composable
private fun AsterAiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Violet,
            onPrimary = Color.White,
            secondary = Color(0xFF827BE2),
            background = Canvas,
            surface = Color.White,
            onSurface = Ink,
            onBackground = Ink
        ),
        content = content
    )
}

@Composable
private fun AsterAiApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("aster_ai_session", android.content.Context.MODE_PRIVATE)
    }
    var token by rememberSaveable { mutableStateOf(preferences.getString("token", "").orEmpty()) }
    var email by rememberSaveable { mutableStateOf(preferences.getString("email", "").orEmpty()) }
    var name by rememberSaveable { mutableStateOf(preferences.getString("name", "").orEmpty()) }
    var photoUri by rememberSaveable { mutableStateOf(preferences.getString("photoUri", "").orEmpty()) }

    if (token.isBlank()) {
        AuthScreen(
            onAuthenticated = { result ->
                preferences.edit()
                    .putString("token", result.token)
                    .putString("email", result.email)
                    .putString("name", result.name)
                    .apply()
                token = result.token
                email = result.email
                name = result.name
            }
        )
    } else {
        ChatScreen(
            token = token,
            email = email,
            name = name,
            photoUri = photoUri,
            onUpdateProfile = { newName, newPhotoUri ->
                preferences.edit()
                    .putString("name", newName)
                    .putString("photoUri", newPhotoUri)
                    .apply()
                name = newName
                photoUri = newPhotoUri
            },
            onSignOut = {
                preferences.edit().clear().apply()
                token = ""
                email = ""
                name = ""
                photoUri = ""
            }
        )
    }
}

@Composable
private fun AuthScreen(onAuthenticated: (AuthResult) -> Unit) {
    var createAccount by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
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
            Text("Aster AI", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text(
                "Your intelligent companion.",
                color = Muted,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        if (createAccount) "Create your account" else "Welcome back",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    )
                    Text(
                        if (createAccount) "Your chats will be saved securely." else "Sign in to continue your conversations.",
                        color = Muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (createAccount) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; error = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Name") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (createAccount) ImeAction.Next else ImeAction.Done
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                    if (createAccount) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; error = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirm Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Text(
                            "Use at least 6 characters.",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    error?.let { InlineError(it) }
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank() || (createAccount && (name.isBlank() || confirmPassword.isBlank()))) {
                                error = "Please fill in all fields."
                                return@Button
                            }
                            if (createAccount && password != confirmPassword) {
                                error = "Passwords do not match."
                                return@Button
                            }
                            busy = true
                            error = null
                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        if (createAccount) ChatApi.register(name.trim(), email.trim(), password)
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
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(19.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (createAccount) "Create account" else "Sign in", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    TextButton(
                        onClick = {
                            createAccount = !createAccount
                            error = null
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            if (createAccount) "Already have an account? Sign in"
                            else "New here? Create an account"
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun BrandMark() {
    Surface(
        modifier = Modifier.size(58.dp),
        shape = CircleShape,
        color = Violet
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 27.sp)
        }
    }
}

@Composable
private fun ChatScreen(
    token: String, 
    email: String, 
    name: String, 
    photoUri: String, 
    onUpdateProfile: (String, String) -> Unit, 
    onSignOut: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    var chats by remember { mutableStateOf(emptyList<ChatItem>()) }
    var activeChatId by rememberSaveable { mutableStateOf("") }
    var messages by remember { mutableStateOf(emptyList<ChatMessage>()) }
    val provider = "auto"
    var prompt by rememberSaveable { mutableStateOf("") }
    var loadingChats by remember { mutableStateOf(true) }
    var loadingMessages by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val activeChat = chats.firstOrNull { it.id == activeChatId }
    val drawerState = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)

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
            val selected = withContext(Dispatchers.IO) {
                ChatApi.listMessages(token, activeChatId)
            }
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

    if (showSettings) {
        SettingsScreen(
            name = name,
            email = email,
            photoUri = photoUri,
            onClose = { showSettings = false },
            onUpdateProfile = onUpdateProfile,
            onSignOut = onSignOut
        )
        return
    }

    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            androidx.compose.material3.ModalDrawerSheet(
                drawerContainerColor = AppDarkGrey,
                modifier = Modifier.width(280.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        BrandMarkSmall()
                        Spacer(Modifier.width(12.dp))
                        Text("aster", fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Color.White)
                    }
                    Button(
                        onClick = {
                            error = null
                            scope.launch {
                                drawerState.close()
                                try {
                                    val created = withContext(Dispatchers.IO) {
                                        ChatApi.createChat(token, provider)
                                    }
                                    chats = listOf(created) + chats
                                    activeChatId = created.id
                                    messages = emptyList()
                                } catch (exception: Exception) {
                                    error = exception.message
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AppLightGrey, contentColor = Color.White)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("+ New chat", fontWeight = FontWeight.Medium)
                        }
                    }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("HISTORY", modifier = Modifier.padding(horizontal = 20.dp), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        items(chats, key = { it.id }) { chat ->
                            val isActive = chat.id == activeChatId
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                onClick = {
                                    activeChatId = chat.id
                                    error = null
                                    scope.launch { drawerState.close() }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isActive) Color(0xFFEBEBEB) else Color.Transparent
                            ) {
                                Text(
                                    chat.title,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    color = if (isActive) Ink else Color(0xFF444444),
                                    maxLines = 1,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFEAE8F1))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (photoUri.isNotBlank()) {
                            coil.compose.AsyncImage(
                                model = photoUri,
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = Violet
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(name.takeIf { it.isNotBlank() }?.take(1)?.uppercase() ?: "U", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name.ifBlank { "User" }, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp)
                        }
                        androidx.compose.material3.IconButton(onClick = { 
                            scope.launch { drawerState.close() }
                            showSettings = true 
                        }) {
                            Text("⚙\uFE0F", fontSize = 20.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = AppDarkGrey) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Text("☰", fontSize = 24.sp, color = Color.White)
                    }
                    Text(
                        "aster",
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    androidx.compose.material3.IconButton(onClick = { showSettings = true }) {
                        Text("⚙\uFE0F", fontSize = 20.sp, color = Color.White)
                    }
                }
                HorizontalDivider(color = AppLightGrey)

                if (messages.isEmpty() && !loadingMessages) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(26.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BrandMark()
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "What can I help you with?",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (loadingMessages && messages.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppAccentGreen)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            MessageBubble(message)
                        }
                        if (sending) {
                            item(key = "typing-indicator") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    BrandMarkSmall()
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("aster", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                        Spacer(Modifier.height(4.dp))
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = AppAccentGreen,
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Message aster...", color = Muted) },
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Default
                        ),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AppLightGrey,
                            unfocusedContainerColor = AppLightGrey,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        trailingIcon = {
                            androidx.compose.material3.IconButton(
                                onClick = {
                                    var chatId = activeChatId
                                    val text = prompt.trim()
                                    if (text.isBlank() || sending) return@IconButton
                                    prompt = ""
                                    sending = true
                                    error = null
                                    scope.launch {
                                        try {
                                            if (chatId.isBlank()) {
                                                val created = withContext(Dispatchers.IO) {
                                                    ChatApi.createChat(token, provider)
                                                }
                                                chats = listOf(created) + chats
                                                chatId = created.id
                                                activeChatId = chatId
                                            }
                                            val optimistic = ChatMessage(
                                                id = "local-${UUID.randomUUID()}",
                                                chatId = chatId,
                                                role = "user",
                                                content = text,
                                                provider = provider,
                                                createdAt = System.currentTimeMillis()
                                            )
                                            messages = messages + optimistic
                                            
                                            val result = withContext(Dispatchers.IO) {
                                                ChatApi.sendMessage(token, chatId, provider, text)
                                            }
                                            messages = messages.dropLast(1) + result.userMessage + result.assistantMessage
                                            chats = withContext(Dispatchers.IO) { ChatApi.listChats(token) }
                                        } catch (exception: Exception) {
                                            if (chatId.isNotBlank()) {
                                                try {
                                                    withContext(Dispatchers.IO) {
                                                        messages = ChatApi.listMessages(token, chatId)
                                                    }
                                                } catch (_: Exception) {}
                                            }
                                            error = exception.message ?: "Aster server had a problem."
                                        } finally {
                                            sending = false
                                        }
                                    }
                                },
                                enabled = prompt.isNotBlank() && !sending,
                                modifier = Modifier.padding(end = 4.dp).background(if (prompt.isNotBlank() && !sending) AppAccentGreen else AppLightGrey, CircleShape).size(36.dp)
                            ) {
                                Text("↑", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandMarkSmall() {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = CircleShape,
        color = Violet
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
        if (isUser) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = 280.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = AppLightGrey,
                ) {
                    Text(
                        message.content,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                BrandMarkSmall()
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("aster", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Material3RichText(
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        Markdown(content = message.content)
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

private fun Long.asDateLabel(): String {
    if (this <= 0L) return "Saved"
    val formatter = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(this))
}

@Composable
private fun SettingsScreen(
    name: String,
    email: String,
    photoUri: String,
    onClose: () -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onSignOut: () -> Unit
) {
    var isDarkTheme by remember { mutableStateOf(false) } 
    var selectedLanguage by remember { mutableStateOf("English") }
    var showInfoDialog by remember { mutableStateOf<String?>(null) }
    
    var editName by remember { mutableStateOf(name) }
    var editPassword by remember { mutableStateOf("") }
    
    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateProfile(editName, uri.toString())
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Canvas) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.IconButton(onClick = onClose) {
                        Text("←", fontSize = 24.sp, color = Ink)
                    }
                    Text(
                        "Settings",
                        modifier = Modifier.weight(1f).padding(end = 48.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Ink
                    )
                }
                HorizontalDivider(color = Color(0xFFF0F0F0))
                
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clickable {
                                        photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                            ) {
                                if (photoUri.isNotBlank()) {
                                    AsyncImage(
                                        model = photoUri,
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = CircleShape,
                                        color = Violet
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(name.takeIf { it.isNotBlank() }?.take(1)?.uppercase() ?: "U", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                                        }
                                    }
                                }
                                Surface(
                                    modifier = Modifier.align(Alignment.BottomEnd).size(24.dp),
                                    shape = CircleShape,
                                    color = Color.White,
                                    shadowElevation = 2.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("✏\uFE0F", fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { 
                                    editName = it
                                    onUpdateProfile(it, photoUri)
                                },
                                label = { Text("Display Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = editPassword,
                                onValueChange = { editPassword = it },
                                label = { Text("New Password (Optional)") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Enter to change password") },
                                shape = RoundedCornerShape(14.dp)
                            )
                            if (editPassword.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { 
                                        editPassword = ""
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Update Password")
                                }
                            }
                        }
                    }

                    item {
                        Text("PREFERENCES", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        SettingsRow(icon = "🌗", title = "Theme", value = if (isDarkTheme) "Dark" else "Light") {
                            isDarkTheme = !isDarkTheme
                        }
                        SettingsRow(icon = "\uD83C\uDF10", title = "Language", value = selectedLanguage) {
                            selectedLanguage = if (selectedLanguage == "English") "Spanish" else "English"
                        }
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        Text("ABOUT", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        SettingsRow(icon = "\uD83D\uDCC4", title = "Privacy Policy") { showInfoDialog = "Privacy Policy" }
                        SettingsRow(icon = "\uD83D\uDCDC", title = "Terms of Service") { showInfoDialog = "Terms of Service" }
                        SettingsRow(icon = "✉\uFE0F", title = "Contact Us") { showInfoDialog = "Contact Us" }
                    }

                    item {
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = onSignOut,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(50.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Text("Sign out", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }

            if (showInfoDialog != null) {
                AlertDialog(
                    onDismissRequest = { showInfoDialog = null },
                    title = { Text(showInfoDialog!!) },
                    text = {
                        Text(
                            when (showInfoDialog) {
                                "Privacy Policy" -> "Aster AI values your privacy. We do not sell your personal data. All conversations are stored securely on our servers."
                                "Terms of Service" -> "By using Aster AI, you agree to interact respectfully. We reserve the right to suspend accounts violating these terms."
                                "Contact Us" -> "Need help? Email us at support@asterai.com or visit our website at asterai.com."
                                else -> ""
                            }
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showInfoDialog = null }) { Text("Close") }
                    },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = Color.White
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(icon: String, title: String, value: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), fontSize = 16.sp, color = Ink)
        if (value != null) {
            Text(value, color = Muted, fontSize = 14.sp)
        }
    }
}
