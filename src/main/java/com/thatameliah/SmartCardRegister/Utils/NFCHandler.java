package com.thatameliah.SmartCardRegister.Utils;

import java.math.BigInteger;
import java.util.List;
import javax.smartcardio.*;

public class NFCHandler {
    public static void main(String[] args) throws CardException {
        final TerminalFactory factory = TerminalFactory.getDefault();
        final List<CardTerminal> terminals = factory.terminals().list();

        CardTerminal terminal = terminals.getFirst();
        System.out.println("Connected to terminal \"" + terminal + "\" (index 0 of " + (terminals.size() - 1) + ")\n");
        
        while (true) {
            System.out.println("----------------------\nWaiting for card...");
            terminal.waitForCardPresent(0);
            
            Card card = terminal.connect("*");
            System.out.println("Card: " + card);

            ATR atr = card.getATR();
            byte[] atrBytes = atr.getBytes();

            System.out.print("ATR: ");
            for (byte b : atrBytes) System.out.printf("%02X ", b);
            System.out.println("\nProtocol: " + card.getProtocol() + "\n");

            CardChannel channel = card.getBasicChannel();
            CommandAPDU command = new CommandAPDU(ToByteArray(new int[]{0xFF, 0xCA, 0x00, 0x00, 0x00}));
            ResponseAPDU response = channel.transmit(command);
            System.out.println("Response: " + response);
            if (response.getSW1() == 0x63 && response.getSW2() == 0x00) System.err.println("Read Failed!");

            System.out.println("\nUID: " + ToHex(response.getData()));
            card.disconnect(false);

            System.out.println("\nWaiting for card disconnect...");
            terminal.waitForCardAbsent(0);
            System.out.println("----------------------\n");
        }
    }
    
    private static byte[] ToByteArray(int[] nums) {
        byte[] bytes = new byte[nums.length];
        for (int i = 0; i < nums.length; i++) {
            bytes[i] = (byte) nums[i];
        }
        return bytes;
    }
    
    private static String ToHex(byte[] bytes) {
        return String.format("%0" + (bytes.length * 2) + "X", new BigInteger(1, bytes));
    }
}
