package com.thatameliah.SmartCardRegister.Utils

import com.thatameliah.SmartCardRegister.Forms.Register

import java.math.BigInteger
import javax.smartcardio._
import java.util

object NFCHandler {
  private val standardCommand: Array[Int] = Array(0xFF, 0xCA, 0x00, 0x00, 0x00)
  private val factory: TerminalFactory = TerminalFactory.getDefault

  private var terminals: util.List[CardTerminal] = new util.ArrayList[CardTerminal]()
  private var cardTerminal: Option[CardTerminal] = None
  RefreshTerminals

  /**
   * One full pass of the PC/SC handshake to get a UID from a presented smart card. Protected via a status check.
   * @param cardPresentTimeout The amount of time to wait for a card to be presented to the reader. 0 waits indefinitely.
   * @param cardAbsentTimeout The amount of time to wait for the card to be removed. 0 waits indefinitely and is recommended.
   * @param register The register object invoking this method.
   * @return The UID of the presented card as a String of Hex values
   */
  def GetUIDFromCard(cardPresentTimeout: Long, cardAbsentTimeout: Long, register: Register): String = {
    if (register.status != Register.Status.READY) return new String
    if (cardTerminal.isEmpty) return new String
    
    val terminal = cardTerminal.get
    terminal.waitForCardPresent(cardPresentTimeout)

    val card: Card = ConnectToCard
    if (card == null) return new String

    val response: ResponseAPDU = TransmitCommand(card, standardCommand)
    if (response == null) return new String
    if (response.getSW1 == 0x63 && response.getSW2 == 0x00) return new String
    val UID: Array[Byte] = response.getData

    register.SetStatus(Register.Status.WAITING_FOR_CARD_ABSENT)
    terminal.waitForCardAbsent(cardPresentTimeout)
    ToHexString(UID)
  }

  /**
   * One full pass of the PC/SC handshake to get a UID from a presented smart card. Runs unconditionally to test the connected terminal
   * @param cardPresentTimeout The amount of time to wait for a card to be presented to the reader. 0 waits indefinitely.
   * @param cardAbsentTimeout The amount of time to wait for the card to be removed. 0 waits indefinitely and is recommended.
   * @return The UID of the presented card as a String of Hex values
   */
  def TestTerminal(cardPresentTimeout: Long, cardAbsentTimeout: Long): String = {
    if (cardTerminal.isEmpty) return new String
    val terminal = cardTerminal.get

    terminal.waitForCardPresent(cardPresentTimeout)

    val card: Card = ConnectToCard
    if (card == null) return new String

    val response: ResponseAPDU = TransmitCommand(card, standardCommand)
    if (response == null) return new String
    if (response.getSW1 == 0x63 && response.getSW2 == 0x00) return new String
    val UID: Array[Byte] = response.getData

    terminal.waitForCardAbsent(cardAbsentTimeout)
    ToHexString(UID)
  }

  /**
   * Connect to a smart card using a wildcard protocol.
   * @return The card that has been connected
   */
  private def ConnectToCard: Card = {
    try cardTerminal match {
      case Some(terminal) => terminal.connect("*")
      case None => null
    } catch {
      case _: CardException => null
    }
  }

  /**
   * Transmits a command to the presented card before disconnecting it.
   * @param card The card to transmit to
   * @param commandArray The command to transmit to the card
   * @return The response from the card
   */
  private def TransmitCommand(card: Card, commandArray: Array[Int]): ResponseAPDU = {
    val channel: CardChannel = card.getBasicChannel
    val command: CommandAPDU = new CommandAPDU(ToByteArray(commandArray))

    try channel.transmit(command)
    catch { case _: CardException => null }
    finally card.disconnect(false) // Ensure the card is always disconnected, even if the program crashes unexpectedly.
  }

  // Data manipulation methods.
  private def ToByteArray(nums: Array[Int]): Array[Byte] = nums.map(_.toByte)
  private def ToHexString(bytes: Array[Byte]): String = String.format("%0" + (bytes.length * 2) + "X", new BigInteger(1, bytes))

  /**
   * Gets the list of connected terminals. It is advised to refresh terminals before calling this function, to prevent disconnected terminals from being returned.
   * @return The list (java.util.List) of CardTerminal objects currently connected.
   */
  def GetConnectedTerminals: util.List[CardTerminal] = terminals

  /**
   * Sets the active terminal to the integer value provided. If newTerminal is None, the first terminal (position 0) will be selected.
   * @param newTerminal An optional integer representing the index of the new terminal. Defaults to 0.
   */
  def SetActiveTerminal(newTerminal: Option[Integer]): Unit = {
    val fallback: Integer = Integer.valueOf(0)
    val terminalNumber: Integer = newTerminal.getOrElse(fallback)
    cardTerminal = Some(terminals.get(terminalNumber))
  }

  /**
   * Gets the name of the currently active terminal
   * @return The name of the terminal connected, or "N/A" if no terminal is selected.
   */
  def GetActiveTerminalName: String = {
    cardTerminal match {
      case Some(t) => t.getName
      case None => "N/A"
    }
  }

  /**
   * Refreshes the list of connected terminals, before setting the terminal to the first entry in the Terminals list, or None if none are connected.
   * @return The new terminal, this will always be terminals[0], or None if no terminals are connected.
   */
  def RefreshTerminals: Option[CardTerminal] = {
    try terminals = factory.terminals.list
    catch {
      case _: CardException => terminals = new util.ArrayList[CardTerminal]
    }

    cardTerminal = {
      if (terminals.isEmpty) None
      else Some(terminals.get(0))
    }; cardTerminal
  }
}