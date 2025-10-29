package com.thatameliah.SmartCardRegister.Handlers

import java.util.Base64

object Base64Handler {
  private val ENCODER: Base64.Encoder = Base64.getEncoder
  private val DECODER: Base64.Decoder = Base64.getDecoder

  /**
   * Encodes a string into Base 64
   *
   * @param input The String to be encoded
   * @return The input String encoded in Base64
   */
  def EncodeString(input: String): String = {
    if (input == null) return null
    val bytes = input.getBytes
    ENCODER.encodeToString(bytes)
  }

  /**
   * Decodes a string from Base 64 into plaintext
   *
   * @param input The String to be decoded
   * @return The input String decoded into plaintext
   */
  def DecodeString(input: String): String = {
    if (input == null) return null
    val bytes = DECODER.decode(input)
    return new String(bytes)
  }
}