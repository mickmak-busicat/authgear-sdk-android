package com.oursky.authgear

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.security.Signature
import java.time.Instant

internal enum class JWTHeaderType(val value: String) {
    ANONYMOUS("vnd.authgear.anonymous-request"),
    BIOMETRIC("vnd.authgear.biometric-request")
}

internal data class JWTHeader(
    val typ: JWTHeaderType,
    val kid: String,
    val alg: String,
    val jwk: JWK?
)

internal fun JWTHeader.toJsonObject(): JsonObject {
    val header = mutableMapOf<String, JsonElement>()
    header["typ"] = JsonPrimitive(typ.value)
    header["kid"] = JsonPrimitive(kid)
    header["alg"] = JsonPrimitive(alg)
    jwk?.let { jwk ->
        header["jwk"] = jwk.toJsonObject()
    }
    return JsonObject(header)
}

internal data class JWTPayload(
    val iat: Long,
    val exp: Long,
    val challenge: String,
    val action: String
    // FIXME(biometric): device info
) {
    constructor(now: Instant, challenge: String, action: String) : this(
        iat = now.epochSecond,
        exp = now.epochSecond + 60,
        challenge = challenge,
        action = action
    )
}

internal fun JWTPayload.toJsonObject(): JsonObject {
    val m = mutableMapOf<String, JsonElement>()
    m["iat"] = JsonPrimitive(iat)
    m["exp"] = JsonPrimitive(exp)
    m["challenge"] = JsonPrimitive(challenge)
    m["action"] = JsonPrimitive(action)
    return JsonObject(m)
}

internal fun signJWT(signature: Signature, header: JWTHeader, payload: JWTPayload): String {
    val data = "${base64UrlEncode(Json.encodeToString(header.toJsonObject()).toUTF8())}.${base64UrlEncode(Json.encodeToString(payload.toJsonObject()).toUTF8())}"
    signature.update(data.toUTF8())
    val sig = signature.sign()
    return "$data.${base64UrlEncode(sig)}"
}
