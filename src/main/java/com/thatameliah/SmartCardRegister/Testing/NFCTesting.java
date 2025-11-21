package com.thatameliah.SmartCardRegister.Testing;

import com.thatameliah.SmartCardRegister.Utils.NFCHandler;

public class NFCTesting {
    public static void main(String[] args) {
        while (true) {
            System.out.println("----------------------\nWaiting for card...");

            String UID = NFCHandler.GetUIDFromCard(0);
            System.out.println(UID);

            System.out.println("----------------------\n");
        }
    }
}