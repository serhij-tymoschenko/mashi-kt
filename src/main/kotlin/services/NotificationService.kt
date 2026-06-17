package com.mashiverse.services

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import java.nio.file.Paths
import kotlin.io.path.inputStream

object NotificationService {
    private val androidCredPath = Paths.get(System.getProperty("user.dir"), "mash-it-android-app-firebase-adminsdk-fbsvc-bb09c6be56.json")
    private val iosCredPath = Paths.get(System.getProperty("user.dir"), "mash-it-ios-firebase-adminsdk-fbsvc-149787883e.json")

    private val androidApp: FirebaseApp
    private val iosApp: FirebaseApp

    init {
        androidCredPath.inputStream().use { androidStream ->
            val androidOptions = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(androidStream))
                .build()
            androidApp = FirebaseApp.initializeApp(androidOptions)
        }

        iosCredPath.inputStream().use { iosStream ->
            val iosOptions = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(iosStream))
                .build()
            iosApp = FirebaseApp.initializeApp(iosOptions, "ios")
        }
    }

    fun notifyAndroidUsers(title: String, body: String, listingId: String? = null) {
        try {
            val message = Message.builder()
                .putAllData(mapOf(
                    "title" to title,
                    "body" to body,
                    "listingId" to (listingId ?: "")
                ))
                .setTopic("all_users")
                .build()

            FirebaseMessaging.getInstance(androidApp).send(message)
            println("Android notification sent successfully")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun notifyIosUsers(title: String, body: String, listingId: String? = null) {
        try {
            val message = Message.builder()
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build()
                )
                .putData("listingId", listingId ?: "")
                .setApnsConfig(
                    ApnsConfig.builder()
                        .setAps(
                            Aps.builder()
                                .setSound("default")
                                .setBadge(1)
                                .build()
                        )
                        .build()
                )
                .setTopic("ios_users")
                .build()

            FirebaseMessaging.getInstance(iosApp).send(message)
            println("iOS notification sent successfully")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

