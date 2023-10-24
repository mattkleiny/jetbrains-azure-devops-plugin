package com.intellij.tasks.azure

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.http.HttpResponse
import java.time.Instant
import java.util.*

/** Converts a string to Base64 */
fun String.toBase64(): String =
    Base64.getEncoder().encodeToString(this.toByteArray()).trim()

/** Encodes a string to be used in a URL; this is not a full encoding, just enough to get the basics working */
fun String.toUrlEncoded(): String =
    this.replace(" ", "%20")

/** Attempts to parse a date from the given JSON */
fun JsonNode.asDate(): Date? = with(this.asText()) {
    if (this.isEmpty()) {
        return null
    }
    return Date.from(Instant.parse(this))
}

/** Converts any object to a JSON string */
fun Any.toJson(): String =
    jacksonObjectMapper().writeValueAsString(this)

/** Converts an [HttpResponse] to a [JsonNode] for manual parsing */
fun HttpResponse<String>.asJson(): JsonNode =
    jacksonObjectMapper().readTree(this.body())
