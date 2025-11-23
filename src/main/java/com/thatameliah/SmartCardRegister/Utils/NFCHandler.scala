package com.thatameliah.SmartCardRegister.Utils

import javax.smartcardio._
import java.math.BigInteger
import java.util

object NFCHandler {
  private val standardCommand: Array[Int] = Array(0xFF, 0xCA, 0x00, 0x00, 0x00)
  private val factory: TerminalFactory = TerminalFactory.getDefault

  private var terminals: util.List[CardTerminal] = new util.ArrayList[CardTerminal]()
  private var terminal: Option[CardTerminal] = None
  RefreshTerminals()

  /**
   * One full pass of the PC/SC handshake to get a UID from a presented smart card.
   *
   * @param timeout The amount of time to wait for card present and card absent. 0 waits indefinitely.
   * @return The UID of the presented card as a String of Hex values
   */
  def GetUIDFromCard(timeout: Long): String = {
    if (terminal.isEmpty) new String
    val t = terminal.get

    t.waitForCardPresent(timeout)

    val card: Card = ConnectToCard
    if (card == null) new String

    val response: ResponseAPDU = TransmitStandardCommand(card)
    if (response == null) new String
    if (response.getSW1 == 0x63 && response.getSW2 == 0x00) new String
    val UID: Array[Byte] = response.getData

    card.disconnect(false)
    t.waitForCardAbsent(timeout)
    ToHex(UID)
  }

  /**
   * Connect to a smart card using a wildcard protocol.
   *
   * @return The card that has been connected
   */
  private def ConnectToCard: Card = {
    terminal match {
      case Some(t) =>
        try t.connect("*")
        catch { case _: CardException => null }

      case None => null
    }
  }

  /**
   * Transmits the standard command to the presented card
   *
   * @param card The card to transmit to
   * @return The response from the card
   */
  private def TransmitStandardCommand(card: Card): ResponseAPDU = {
    val channel: CardChannel = card.getBasicChannel
    val command: CommandAPDU = new CommandAPDU(ToByteArray(standardCommand))

    try { channel.transmit(command) }
    catch {
      case _: CardException => null
      case err: Exception => card.disconnect(false); throw err
    }
  }

  private def ToByteArray(ints: Array[Int]): Array[Byte] = ints.map(_.toByte)
  private def ToHex(bytes: Array[Byte]): String = String.format("%0" + (bytes.length * 2) + "X", new BigInteger(1, bytes))

  def GetTerminals: util.List[CardTerminal] = terminals
  def SetActiveTerminal(newTerminal: Int): Unit = terminal = Some(terminals.get(newTerminal))

  def GetActiveTerminalName: String = {
    terminal match {
      case Some(t) => t.getName
      case None => "N/A"
    }
  }

  def RefreshTerminals(): Unit = {
    try terminals = factory.terminals.list
    catch { case _: CardException => terminals = new util.ArrayList[CardTerminal]() }

    terminal =
      if (!terminals.isEmpty) Some(terminals.get(0))
      else None
  }
}