package com.thatameliah.RFIDRegister;

import com.thatameliah.RFIDRegister.Forms.MainRegister;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                System.out.println("Loading MainRegister on " + Thread.currentThread());
                MainRegister register = new MainRegister();
                register.setVisible(true);
            }
        });
    }
}