package com.thatameliah.SmartCardRegister.Utils

import org.jetbrains.annotations.{NotNull, Nullable}

import java.util.Base64
import java.util.Base64.{Decoder, Encoder}

object Base64Handler {
  private val ENCODER: Encoder = Base64.getEncoder
  private val DECODER: Decoder = Base64.getDecoder

  /**
   * Encodes a string into Base 64
   * @param input The String to be encoded
   * @return The input String encoded in Base64
   */
  @NotNull def EncodeString(@Nullable input: String): String = {
    if (input == null || input.isEmpty) new String(ENCODER.encode("".getBytes()))
    else new String(ENCODER.encode(input.getBytes))
  }

  /**
   * Decodes a string from Base 64 into plaintext
   * @param input The String to be decoded
   * @return The input String decoded into plaintext
   */
  @NotNull def DecodeString(@NotNull input: String): String = {
    if (input == null || input.isEmpty) new String
    else new String(DECODER.decode(input))
  }
}