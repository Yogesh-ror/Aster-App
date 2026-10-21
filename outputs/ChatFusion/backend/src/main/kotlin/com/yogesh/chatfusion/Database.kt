package com.yogesh.chatfusion

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import org.bson.Document
import kotlin.concurrent.thread

object Database {
    private var client: MongoClient? = null
    lateinit var db: MongoDatabase
        private set

    val users: MongoCollection<Document>
        get() = db.getCollection("users")

    val chats: MongoCollection<Document>
        get() = db.getCollection("chats")

    val messages: MongoCollection<Document>
        get() = db.getCollection("messages")

    fun initialize(uri: String, dbName: String) {
        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(uri))
            .applyToSslSettings { ssl ->
                ssl.enabled(true)
                ssl.invalidHostNameAllowed(true)
            }
            .build()
        val mClient = MongoClients.create(settings)
        client = mClient
        db = mClient.getDatabase(dbName)

        thread(isDaemon = true) {
            runCatching {
                users.createIndex(Indexes.ascending("email"), IndexOptions().unique(true))
                chats.createIndex(Indexes.ascending("userId"))
                messages.createIndex(Indexes.compoundIndex(Indexes.ascending("chatId"), Indexes.ascending("createdAt")))
            }
        }
    }
}
