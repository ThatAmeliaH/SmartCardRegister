package com.thatameliah.SmartCardRegister.Tests;

import com.thatameliah.SmartCardRegister.Utils.NFCHandler;

import javax.smartcardio.CardException;
import javax.smartcardio.CardNotPresentException;

public class NFCTesting {
  /**
   * Secondary entry point for the program. Starts into an NFC reader testing version.
   */
  public static void main(String[] args) throws CardException {
    while (true) {
      System.out.println("----------------------\nWaiting for card...");

      String UID = NFCHandler.TestTerminal(0, 0);
      if (UID.isEmpty()) {throw new CardNotPresentException("No card was presented within the allocated timeout.");}
      System.out.println("UID: " + UID);

      System.out.println("----------------------\n");
    }
  }
}