package by.peekhovsky.spolks;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * @author Rastsislau Piakhouski 2019
 */
@SuppressWarnings("WeakerAccess")
@Slf4j
public abstract class BaseUdpConnection implements Connection {

  private static final long NANOS_IN_SEC = (long) Math.pow(10, 9);

  protected final int timeoutInMs;
  protected final int bufferSize;

  protected long uploadedOffset = 0;

  @Setter
  protected DatagramSocket socket;

  @Setter
  protected InetAddress inetAddress;

  @Setter
  protected boolean isExit = false;

  @Setter
  protected int port;

  public BaseUdpConnection(DatagramSocket socket, int timeoutInMs, int bufferSize, InetAddress inetAddress, int port) {
    this.timeoutInMs = timeoutInMs;
    this.bufferSize = bufferSize;
    this.socket = socket;
    try {
      this.socket.setSoTimeout(timeoutInMs);
    } catch (SocketException e) {
      e.printStackTrace();
    }
    this.inetAddress = inetAddress;
    this.port = port;
  }

  public BaseUdpConnection(DatagramSocket socket, int timeoutInMs, int bufferSize) {
    this.timeoutInMs = timeoutInMs;
    this.bufferSize = bufferSize;
    this.socket = socket;
    try {
      this.socket.setSoTimeout(timeoutInMs);
    } catch (SocketException e) {
      e.printStackTrace();
    }
    this.inetAddress = socket.getInetAddress();
  }

  @Override
  public void changeSocket(DatagramSocket newSocket) throws IOException {
    this.socket = newSocket;
    socket.setSoTimeout(timeoutInMs);
  }

  @Override
  public void sendString(String str) throws IOException {
    DatagramPacket datagramPacket
        = new DatagramPacket(str.getBytes(), str.length(), inetAddress, port);
    socket.send(datagramPacket);
  }

  @Override
  public String receiveString() throws IOException {
    byte[] buffer = new byte[bufferSize];
    DatagramPacket packet = new DatagramPacket(buffer, bufferSize);

    try {
      socket.receive(packet);
    } catch (SocketTimeoutException e) {
      log.warn("[RECEIVE] Timeout");
      return "";
    }
    return new String(packet.getData(), 0, packet.getLength());
  }


  public String sendWithReceive(String str, int attemptNum) throws IOException {
    while (true) {
      sendString(str);
      var receivedStr = receiveString();
      if (!receivedStr.isEmpty()) {
        return receivedStr;
      }
      attemptNum--;
      if (attemptNum == 0) {
        isExit = true;
        return "";
      }
    }
  }

  @Override
  public void setExit(boolean exit) {
    this.isExit = exit;
  }

  protected double calcBitrate(int numOfReceivedBytes, long timeInNanos) {
    return (numOfReceivedBytes * 8.0) / ((double) timeInNanos / NANOS_IN_SEC);
  }
}
