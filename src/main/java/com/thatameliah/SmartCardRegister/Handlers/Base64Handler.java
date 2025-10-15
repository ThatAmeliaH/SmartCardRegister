package com.thatameliah.SmartCardRegister.Handlers;

import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Base64.Decoder;

public class Base64Handler {
    private final Encoder encoder = Base64.getEncoder();
    private final Decoder decoder = Base64.getDecoder();

    public String EncodeString(String input) {
        byte[] bytes = input.getBytes();
        return encoder.encodeToString(bytes);
    }

    public String DecodeString(String input) {
        byte[] bytes = decoder.decode(input);
        return new String(bytes);
    }
}