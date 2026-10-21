package com.yogesh.chatfusion

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.bson.Document
import org.bson.types.ObjectId
import org.mindrot.jbcrypt.BCrypt
import java.util.Date

fun main() {
    val mongoUri = System.getenv("MONGODB_URI") ?: "mongodb://localhost:27017"
    val mongoDbName = System.getenv("MONGODB_DATABASE") ?: "chatfusion"
    val jwtSecret = System.getenv("JWT_SECRET") ?: "default_secret_key_needs_32_chars_minimum"
    val openAiKey = System.getenv("OPENAI_API_KEY") ?: ""
    val openAiModel = System.getenv("OPENAI_MODEL") ?: "gpt-4o"
    val geminiKey = System.getenv("GEMINI_API_KEY") ?: ""
    val geminiModel = System.getenv("GEMINI_MODEL") ?: "gemini-1.5-flash"
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    kotlin.concurrent.thread {
        runCatching {
            Database.initialize(mongoUri, mongoDbName)
        }
    }
    val providerClient = ProviderClient(openAiKey, openAiModel, geminiKey, geminiModel)

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        chatFusionModule(jwtSecret, providerClient)
    }.start(wait = true)
}

fun Application.chatFusionModule(jwtSecret: String, providerClient: ProviderClient) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        })
    }

    val algorithm = Algorithm.HMAC256(jwtSecret)

    fun createToken(userId: String, email: String): String {
        return JWT.create()
            .withSubject(userId)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000))
            .sign(algorithm)
    }

    fun verifyToken(token: String): Pair<String, String>? {
        return try {
            val verifier = JWT.require(algorithm).build()
            val decoded = verifier.verify(token)
            val userId = decoded.subject
            val email = decoded.getClaim("email").asString()
            userId to email
        } catch (_: Exception) {
            null
        }
    }

    fun authenticatedUserId(call: ApplicationCall): Pair<String, String>? {
        val authHeader = call.request.headers["Authorization"] ?: return null
        if (!authHeader.startsWith("Bearer ")) return null
        val token = authHeader.removePrefix("Bearer ").trim()
        return verifyToken(token)
    }

    routing {
        post("/v1/auth/register") {
            val request = try {
                call.receive<AuthRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid payload."))
                return@post
            }
            if (request.email.isBlank() || request.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Email and password are required."))
                return@post
            }

            val existing = Database.users.find(Document("email", request.email.lowercase())).firstOrNull()
            if (existing != null) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("Email is already registered."))
                return@post
            }

            val hash = BCrypt.hashpw(request.password, BCrypt.gensalt())
            val userDoc = Document()
                .append("email", request.email.lowercase())
                .append("passwordHash", hash)
                .append("name", request.name ?: "")
                .append("mobileNumber", request.mobileNumber ?: "")
                .append("photoUrl", request.photoUrl ?: "")
                .append("createdAt", System.currentTimeMillis())

            Database.users.insertOne(userDoc)
            val userId = userDoc.getObjectId("_id").toHexString()
            val token = createToken(userId, request.email.lowercase())

            call.respond(
                HttpStatusCode.Created,
                AuthResponse(
                    token = token,
                    userId = userId,
                    email = request.email.lowercase(),
                    name = request.name ?: "",
                    mobileNumber = request.mobileNumber ?: "",
                    photoUrl = request.photoUrl ?: ""
                )
            )
        }

        post("/v1/auth/login") {
            val request = try {
                call.receive<AuthRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid payload."))
                return@post
            }

            val userDoc = Database.users.find(Document("email", request.email.lowercase())).firstOrNull()
            if (userDoc == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials."))
                return@post
            }

            val hash = userDoc.getString("passwordHash") ?: ""
            if (!BCrypt.checkpw(request.password, hash)) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials."))
                return@post
            }

            val userId = userDoc.getObjectId("_id").toHexString()
            val token = createToken(userId, request.email.lowercase())

            call.respond(
                HttpStatusCode.OK,
                AuthResponse(
                    token = token,
                    userId = userId,
                    email = request.email.lowercase(),
                    name = userDoc.getString("name") ?: "",
                    mobileNumber = userDoc.getString("mobileNumber") ?: "",
                    photoUrl = userDoc.getString("photoUrl") ?: ""
                )
            )
        }

        get("/v1/profile") {
            val (userId, _) = authenticatedUserId(call) ?: run {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
                return@get
            }

            val userDoc = Database.users.find(Document("_id", ObjectId(userId))).firstOrNull()
            if (userDoc == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
                return@get
            }

            call.respond(
                HttpStatusCode.OK,
                UserProfileDto(
                    userId = userId,
                    email = userDoc.getString("email") ?: "",
                    name = userDoc.getString("name") ?: "",
                    mobileNumber = userDoc.getString("mobileNumber") ?: "",
                    photoUrl = userDoc.getString("photoUrl") ?: ""
                )
            )
        }

        put("/v1/profile") {
            val (userId, _) = authenticatedUserId(call) ?: run {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
                return@put
            }

            val request = try {
                call.receive<UpdateProfileRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid payload."))
                return@put
            }

            val updateFields = Document()
            request.name?.let { updateFields.append("name", it) }
            request.mobileNumber?.let { updateFields.append("mobileNumber", it) }
            request.photoUrl?.let { updateFields.append("photoUrl", it) }
            request.password?.takeIf { it.isNotBlank() }?.let { newPassword ->
                updateFields.append("passwordHash", BCrypt.hashpw(newPassword, BCrypt.gensalt()))
            }

            if (updateFields.isNotEmpty()) {
                Database.users.updateOne(
                    Document("_id", ObjectId(userId)),
                    Document("\$set", updateFields)
                )
            }

            val updatedUser = Database.users.find(Document("_id", ObjectId(userId))).firstOrNull()
            call.respond(
                HttpStatusCode.OK,
                UserProfileDto(
                    userId = userId,
                    email = updatedUser?.getString("email") ?: "",
                    name = updatedUser?.getString("name") ?: "",
                    mobileNumber = updatedUser?.getString("mobileNumber") ?: "",
                    photoUrl = updatedUser?.getString("photoUrl") ?: ""
                )
            )
        }

        get("/v1/chats") {
            val (userId, _) = authenticatedUserId(call) ?: run {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
                return@get
            }

            val userChats = Database.chats.find(Document("userId", userId))
                .map { doc ->
                    ChatDto(
                        id = doc.getObjectId("_id").toHexString(),
                        title = doc.getString("title") ?: "New chat",
                        provider = doc.getString("provider") ?: "auto",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                }.toList()

            call.respond(HttpStatusCode.OK, userChats)
        }

        post("/v1/chats") {
            val (userId, _) = authenticatedUserId(call) ?: run {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
                return@post
            }

            val req = runCatching { call.receive<ChatCreateRequest>() }.getOrNull()
            val provider = req?.provider ?: "auto"

            val doc = Document()
                .append("userId", userId)
                .append("title", "New chat")
                .append("provider", provider)
                .append("createdAt", System.currentTimeMillis())
                .append("updatedAt", System.currentTimeMillis())

            Database.chats.insertOne(doc)
            val chatId = doc.getObjectId("_id").toHexString()

            call.respond(
                HttpStatusCode.Created,
                ChatDto(
                    id = chatId,
                    title = "New chat",
                    provider = provider,
                    createdAt = doc.getLong("createdAt"),
                    updatedAt = doc.getLong("updatedAt")
                )
            )
        }

        get("/v1/chats/{chatId}/messages") {
            val (userId, _) = authenticatedUserId(call) ?: run {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
                return@get
            }

            val chatId = call.parameters["chatId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val chatDoc = Database.chats.find(Document("_id", ObjectId(chatId)).append("userId", userId)).firstOrNull()
            if (chatDoc == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Chat not found"))
                return@get
            }

            val msgs = Database.messages.find(Document("chatId", chatId))
                .map { doc ->
                    MessageDto(
                        id = doc.getObjectId("_id").toHexString(),
                        chatId = doc.getString("chatId"),
                        role = doc.getString("role"),
                        content = doc.getString("content"),
                        provider = doc.getString("provider") ?: "auto",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }.toList()

            call.respond(HttpStatusCode.OK, msgs)
        }

        post("/v1/chats/{chatId}/messages") {
            val (userId, _) = authenticatedUserId(call) ?: run {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
                return@post
            }

            val chatId = call.parameters["chatId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val chatDoc = Database.chats.find(Document("_id", ObjectId(chatId)).append("userId", userId)).firstOrNull()
            if (chatDoc == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Chat not found"))
                return@post
            }

            val req = try {
                call.receive<SendMessageRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid payload."))
                return@post
            }

            val requestedProvider = req.provider ?: chatDoc.getString("provider") ?: "auto"

            // Save User Message
            val userMsgDoc = Document()
                .append("chatId", chatId)
                .append("userId", userId)
                .append("role", "user")
                .append("content", req.message)
                .append("provider", requestedProvider)
                .append("createdAt", System.currentTimeMillis())
            Database.messages.insertOne(userMsgDoc)

            // Fetch History
            val historyDocs = Database.messages.find(Document("chatId", chatId)).toList()
            val history = historyDocs.map {
                ConversationTurn(role = it.getString("role"), content = it.getString("content"))
            }.dropLast(1) // exclude current user message

            // Race providers if provider is "auto", or use specific provider if set/locked
            val (winningProvider, assistantReply) = try {
                providerClient.generateResponse(requestedProvider, history, req.message)
            } catch (e: Exception) {
                Database.messages.deleteOne(Document("_id", userMsgDoc.getObjectId("_id")))
                val errMsg = e.message ?: "Unknown error"
                val friendlyError = when {
                    errMsg.contains("OpenAI", ignoreCase = true) || errMsg.contains("quota", ignoreCase = true) || errMsg.contains("429") -> 
                        "There is an error in Fastest response mode. Please try your query in Deep thinking mode."
                    errMsg.contains("Gemini", ignoreCase = true) -> 
                        "There is an error in Deep thinking mode. Please try your query in Fastest response mode."
                    else -> "Aster server had a problem: $errMsg"
                }
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(friendlyError))
                return@post
            }

            // Lock this winning provider in MongoDB for this chat session
            if (chatDoc.getString("provider") == "auto" || chatDoc.getString("provider") == "fastest") {
                Database.chats.updateOne(
                    Document("_id", ObjectId(chatId)),
                    Document("\$set", Document("provider", winningProvider))
                )
            }

            // Save Assistant Message with winning provider
            val assistantMsgDoc = Document()
                .append("chatId", chatId)
                .append("userId", userId)
                .append("role", "assistant")
                .append("content", assistantReply)
                .append("provider", winningProvider)
                .append("createdAt", System.currentTimeMillis())
            Database.messages.insertOne(assistantMsgDoc)

            // Update chat title to reflect user query
            val queryText = req.message.lines()
                .firstOrNull { !it.startsWith("[Attached File:") && !it.startsWith("File Content:") && it.isNotBlank() }
                ?.take(30)?.trim() ?: req.message.take(30).trim()

            val titleToSet = queryText.ifBlank { "New chat" }

            Database.chats.updateOne(
                Document("_id", ObjectId(chatId)),
                Document("\$set", Document("title", titleToSet).append("updatedAt", System.currentTimeMillis()))
            )

            val userMsgDto = MessageDto(
                id = userMsgDoc.getObjectId("_id").toHexString(),
                chatId = chatId,
                role = "user",
                content = req.message,
                provider = winningProvider,
                createdAt = userMsgDoc.getLong("createdAt")
            )

            val assistantMsgDto = MessageDto(
                id = assistantMsgDoc.getObjectId("_id").toHexString(),
                chatId = chatId,
                role = "assistant",
                content = assistantReply,
                provider = winningProvider,
                createdAt = assistantMsgDoc.getLong("createdAt")
            )

            call.respond(
                HttpStatusCode.OK,
                SendMessageResponse(userMessage = userMsgDto, assistantMessage = assistantMsgDto)
            )
        }
    }
}
