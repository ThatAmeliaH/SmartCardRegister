package com.thatameliah.SmartCardRegister.Tests;

import com.thatameliah.SmartCardRegister.Utils.NFCHandler;

public class NFCTesting {
  /**
   * Secondary entry point for the program. Starts into an NFC reader testing version.
   */
  public static void main(String[] args) {
    while (true) {
      System.out.println("----------------------\nWaiting for card...");

      String UID = NFCHandler.TestTerminal(0, 0);
      System.out.println("UID: " + UID);

      System.out.println("----------------------\n");
    }
  }
}