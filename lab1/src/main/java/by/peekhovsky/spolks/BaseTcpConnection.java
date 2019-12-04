package by.peekhovsky.spolks;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.time.LocalTime;

/**
 * @author Rastsislau Piakhouski 2019
 */
@SuppressWarnings("WeakerAccess")
@Slf4j
public class BaseTcpConnection {

  private static final long NANOS_IN_SEC = (long) Math.pow(10, 9);

  protected final long timeoutInNanos;
  protected final int bufferSize;

  protected long uploadedOffset = 0;
  protected long downloadedOffset = 0;

  protected Socket socket;
  protected InputStream in;
  protected OutputStream out;
  protected boolean isExit = false;


  public BaseTcpConnection(Socket socket, long timeoutInNanos, int bufferSize) throws IOException {
    this.timeoutInNanos = timeoutInNanos;
    this.bufferSize = bufferSize;
    this.socket = socket;
    this.in = socket.getInputStream();
    this.out = socket.getOutputStream();
  }

  protected void sendString(String str) throws IOException {
    out.write((str + "\n").getBytes());
  }

  protected String receiveString() throws IOException {
    long startTime = LocalTime.now().toNanoOfDay();

    try {
      char ch;
      int chInt;
      var s = new StringBuilder();

      while (true) {
        chInt = in.read();
        ch = (char) chInt;
        if (ch == '\n') {
          return s.toString();
        }

        if (chInt == -1) {
          Thread.sleep(10);
          if (timeoutInNanos < LocalTime.now().toNanoOfDay() - startTime) {
            log.warn("Timeout: {}", timeoutInNanos);
            log.warn("Session timeout: {}", LocalTime.now().toNanoOfDay() - startTime);
            isExit = true;
            return "";
          }
        } else {
          s.append(ch);
          startTime = LocalTime.now().getNano();
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Connection thread has been interrupted");
      isExit = true;
      return "";
    } catch (IOException e) {
      isExit = true;
      return "";
    }
  }

  protected double calcBitrate(int numOfReceivedBytes, long timeInNanos) {
    return  (numOfReceivedBytes * 8.0) / ((double)timeInNanos / NANOS_IN_SEC);
  }

  public void setExit(boolean exit) {
    this.isExit = exit;
  }
}
