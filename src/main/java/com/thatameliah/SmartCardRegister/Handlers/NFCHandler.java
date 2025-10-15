package com.thatameliah.SmartCardRegister.Handlers;

import org.jetbrains.annotations.NotNull;

import javax.smartcardio.*;
import java.util.List;
public class NFCHandler {
    // TODO: Make this static, test at college with my ID Card
    public static void main(String[] args) throws Exception {
        TerminalFactory factory = TerminalFactory.getDefault();
        List<CardTerminal> terminals = factory.terminals().list();

        if (terminals.isEmpty()) {
            System.err.println("No card readers found. Please check your reader drivers and PC/SC service.");
            return;
        }

        CardTerminal terminal = terminals.get(0);
        System.out.println("Using terminal: " + terminal.getName());
        System.out.println("Waiting for card...");

        // Wait indefinitely for card insertion
        terminal.waitForCardPresent(0);

        // Connect to the card
        Card card = terminal.connect("*");
        CardChannel channel = card.getBasicChannel();

        // Get and display the ATR (Answer To Reset)
        System.out.println("Card ATR: " + bytesToHex(card.getATR().getBytes()));

        // Send command to get UID (works on many MIFARE / NFC-A cards)
        // Command: FF CA 00 00 00 — used by PC/SC readers to get the card UID
        byte[] getUidCommand = new byte[] {
                (byte) 0xFF, (byte) 0xCA, (byte) 0x00, (byte) 0x00, (byte) 0x00
        };

        ResponseAPDU response = channel.transmit(new CommandAPDU(getUidCommand));

        if (response.getSW1() == 0x90 && response.getSW2() == 0x00) {
            byte[] uid = response.getData();
            System.out.println("Card UID: " + bytesToHex(uid));
        } else {
            System.err.printf("Failed to read UID. SW1 SW2: %02X %02X%n", response.getSW1(), response.getSW2());
        }

        // Disconnect cleanly
        card.disconnect(false);
        System.out.println("Card disconnected.");
    }

    @NotNull
    private static String bytesToHex(@NotNull byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}