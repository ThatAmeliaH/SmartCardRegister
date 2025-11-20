package com.thatameliah.SmartCardRegister.Utils;

import java.util.List;
import javax.smartcardio.*;

public class NFCHandler {
    public static void main(String[] args) {
        try {
            final TerminalFactory factory = TerminalFactory.getDefault();
            final List<CardTerminal> terminals = factory.terminals().list();
            CardTerminal terminal = terminals.getFirst();
            
            terminal.waitForCardPresent(0);
            
            Card card = terminal.connect("T=0");
            System.out.println("Card: " + card);
            
            CardChannel channel = card.getBasicChannel();
            CommandAPDU command = new CommandAPDU(new byte[]{0x00, (byte) 0xa4, 0x04, 0x00, 0x00});
            ResponseAPDU response = channel.transmit(command);
            System.out.println("Response: " + response);
            
            card.disconnect(false);
            
        } catch (CardException err) {
            System.err.println(err.getMessage());
        }
    }
}
