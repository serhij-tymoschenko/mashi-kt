package com.mashiverse

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.httpsredirect.*

fun Application.configureHttp() {
    install(CORS) {
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }
//    install(HttpsRedirect) {
//        // The port to redirect to. By default 443, the default HTTPS port.
//        sslPort = 443
//        // 301 Moved Permanently, or 302 Found redirect.
//        permanentRedirect = true
//    }
}
