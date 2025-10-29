package com.thatameliah.SmartCardRegister.Handlers;

import java.util.Base64;

@Deprecated
public class Base64Handler {
    private final Base64.Encoder ENCODER = Base64.getEncoder();
    private final Base64.Decoder DECODER = Base64.getDecoder();

    /**
     * Encodes a string into Base 64
     *
     * @param input The String to be encoded
     * @return The input String encoded in Base64
     */
    public String EncodeString(String input) {
        if (input == null) { return null; }
        byte[] bytes = input.getBytes();
        return ENCODER.encodeToString(bytes);
    }

    /**
     * Decodes a string from Base 64 into plaintext
     *
     * @param input The String to be decoded
     * @return The input String decoded into plaintext
     */
    public String DecodeString(String input) {
        if (input == null) { return null; }
        byte[] bytes = DECODER.decode(input);
        return new String(bytes);
    }
}