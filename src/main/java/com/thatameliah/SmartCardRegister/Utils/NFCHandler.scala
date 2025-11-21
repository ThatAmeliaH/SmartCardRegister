package com.thatameliah.SmartCardRegister.Utils

import javax.smartcardio._
import java.math.BigInteger
import java.util

object NFCHandler {
  private val standardCommand: Array[Int] = Array(0xFF, 0xCA, 0x00, 0x00, 0x00)
  private val factory: TerminalFactory = TerminalFactory.getDefault
  private val terminals: util.List[CardTerminal] = factory.terminals.list
  private val terminal = terminals.getFirst

  def GetUIDFromCard(timeout: Long): String = {
    terminal.waitForCardPresent(timeout)

    val card: Card = ConnectToCard
    if (card == null) new String

    val response: ResponseAPDU = TransmitStandardCommand(card)
    if (response == null) new String
    if (response.getSW1 == 0x63 && response.getSW2 == 0x00) new String
    val UID: Array[Byte] = response.getData

    card.disconnect(false)
    terminal.waitForCardAbsent(timeout)
    ToHex(UID)
  }

  private def ConnectToCard: Card = {
    try {
      terminal.connect("*")
    } catch {
      case _: CardException => null
    }
  }

  private def TransmitStandardCommand(card: Card): ResponseAPDU = {
    val channel: CardChannel = card.getBasicChannel
    val command: CommandAPDU = new CommandAPDU(ToByteArray(standardCommand))

    try {
      channel.transmit(command)
    } catch {
      case _: CardException => null
      case err: Exception => card.disconnect(false) throw err
    }
  }

  private def ToByteArray(ints: Array[Int]): Array[Byte] = ints.map(_.toByte)
  private def ToHex(bytes: Array[Byte]): String = String.format("%0" + (bytes.length * 2) + "X", new BigInteger(1, bytes))
}
