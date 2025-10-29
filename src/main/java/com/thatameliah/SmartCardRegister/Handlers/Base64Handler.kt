package com.thatameliah.SmartCardRegister.Handlers

import java.util.*

object Base64Handler {
    private val encoder: Base64.Encoder = Base64.getEncoder()
    private val decoder: Base64.Decoder = Base64.getDecoder()

    /**
     * Encodes a string into Base 64
     *
     * @param input The String to be encoded
     * @return The input String encoded in Base64
     */
    @JvmStatic
    fun encodeString(input: String?): String? {
        if (input == null) {
            return null
        }
        val bytes = input.toByteArray()
        return encoder.encodeToString(bytes)
    }

    /**
     * Decodes a string from Base 64 into plaintext
     *
     * @param input The String to be decoded
     * @return The input String decoded into plaintext
     */
    @JvmStatic
    fun decodeString(input: String?): String? {
        if (input == null) {
            return null
        }
        val bytes: ByteArray = decoder.decode(input)
        return String(bytes)
    }
}