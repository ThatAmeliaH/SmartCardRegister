package com.thatameliah.SmartCardRegister.Handlers;

import java.util.Base64;

public class Base64Handler {
    // Setup ENCODER and DECODER for use in functions
    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    // Private constructor, prevents accidental initialisation
    private Base64Handler() {
        throw new UnsupportedOperationException("Handler classes are static and cannot be initialised.");
    }

    /**
     * Encodes a string into Base 64
     * @param input The String to be encoded
     * @return The input String encoded in Base64
     */
    public static String EncodeString(String input) {
        if (input == null) { return null; }

        byte[] bytes = input.getBytes();
        return ENCODER.encodeToString(bytes);
    }

    /**
     * Decodes a string from Base 64 into plaintext
     * @param input The String to be decoded
     * @return The input String decoded into plaintext
     */
    public static String DecodeString(String input) {
        if (input == null) { return null; }

        byte[] bytes = DECODER.decode(input);
        return new String(bytes);
    }
}