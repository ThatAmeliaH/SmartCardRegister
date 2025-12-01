package com.thatameliah.SmartCardRegister.Utils

import com.thatameliah.SmartCardRegister.Forms.Register

import java.math.BigInteger
import javax.smartcardio._
import java.util

object NFCHandler {
  private val standardCommand: Array[Int] = Array(0xFF, 0xCA, 0x00, 0x00, 0x00)
  private val factory: TerminalFactory = TerminalFactory.getDefault

  private var terminals: util.List[CardTerminal] = new util.ArrayList[CardTerminal]()
  private var terminal: Option[CardTerminal] = None
  RefreshTerminals

  /**
   * One full pass of the PC/SC handshake to get a UID from a presented smart card. Protected via a status check.
   * @param timeout The amount of time to wait for card present and card absent. 0 waits indefinitely.
   * @param register The register object invoking this method.
   * @return The UID of the presented card as a String of Hex values
   */
  def GetUIDFromCard(timeout: Long, register: Register): String = {
    if (!register.status.equals(Register.Status.READY)) return new String
    if (terminal.isEmpty) return new String
    val t = terminal.get

    t.waitForCardPresent(timeout)

    val card: Card = ConnectToCard
    if (card == null) return new String

    val response: ResponseAPDU = TransmitStandardCommand(card)
    if (response == null) return new String
    if (response.getSW1 == 0x63 && response.getSW2 == 0x00) return new String
    val UID: Array[Byte] = response.getData

    register.SetStatus(Register.Status.WAITING_FOR_CARD_ABSENT)
    t.waitForCardAbsent(timeout)
    ToHex(UID)
  }

  /**
   * One full pass of the PC/SC handshake to get a UID from a presented smart card. Runs unconditionally to test the connected terminal
   * @param timeout The amount of time to wait for card present and card absent. 0 waits indefinitely.
   * @return The UID of the presented card as a String of Hex values
   */
  def TestTerminal(timeout: Long): String = {
    if (terminal.isEmpty) return new String
    val t = terminal.get

    t.waitForCardPresent(timeout)

    val card: Card = ConnectToCard
    if (card == null) return new String

    val response: ResponseAPDU = TransmitStandardCommand(card)
    if (response == null) return new String
    if (response.getSW1 == 0x63 && response.getSW2 == 0x00) return new String
    val UID: Array[Byte] = response.getData

    t.waitForCardAbsent(timeout)
    ToHex(UID)
  }

  /**
   * Connect to a smart card using a wildcard protocol.
   * @return The card that has been connected
   */
  private def ConnectToCard: Card = {
    try terminal match {
      case Some(t) => t.connect("*")
      case None => null
    }
    catch { case _: CardException => null }
  }

  /**
   * Transmits the standard command to the presented card
   * @param card The card to transmit to
   * @return The response from the card
   */
  private def TransmitStandardCommand(card: Card): ResponseAPDU = {
    val channel: CardChannel = card.getBasicChannel
    val command: CommandAPDU = new CommandAPDU(ToByteArray(standardCommand))

    try channel.transmit(command)
    catch { case _: CardException => null }
    finally card.disconnect(false)
  }

  // Data manipulation methods.
  private def ToByteArray(nums: Array[Int]): Array[Byte] = nums.map(_.toByte)
  private def ToHex(bytes: Array[Byte]): String = String.format("%0" + (bytes.length * 2) + "X", new BigInteger(1, bytes))

  // Getters and setters
  def GetConnectedTerminals: util.List[CardTerminal] = terminals
  def SetActiveTerminal(newTerminal: Int): Unit = terminal = Some(terminals.get(newTerminal))

  /**
   * Gets the name of the currently active terminal
   * @return The name of the terminal connected, or "N/A" if no terminal is selected.
   */
  def GetActiveTerminalName: String = {
    terminal match {
      case Some(t) => t.getName
      case None => "N/A"
    }
  }

  /**
   * Refreshes the list of connected terminals.
   * @return The new terminal, this will always be terminals[0], or None if no terminals are connected.
   */
  def RefreshTerminals: Option[CardTerminal] = {
    try terminals = factory.terminals.list
    catch {
      case _: CardException => terminals = new util.ArrayList[CardTerminal]
    }

    terminal =
      if (!terminals.isEmpty) Some(terminals.get(0))
      else None
    
    terminal
  }
}