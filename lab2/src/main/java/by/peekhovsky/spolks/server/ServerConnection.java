package by.peekhovsky.spolks.server;

import by.peekhovsky.spolks.BaseUdpConnection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * @author Rastsislau Piakhouski 2019
 */
@Slf4j
@SuppressWarnings("WeakerAccess")
public class ServerConnection extends BaseUdpConnection {

  public ServerConnection(DatagramSocket socket, InetAddress inetAddress, int port) throws IOException {
    super(socket, 5000, 1024, inetAddress, port);
  }

  public void executeSession() throws IOException {
    while (!isExit) {
      var str = receiveString();
      if (str.isEmpty()) {
        continue;
      }
      log.info("received cmd: {}", str);
      execute(str);
    }
  }

  @Override
  public void execute(String cmdWithParams) {
    String[] words = cmdWithParams.split(" ");

    if (words.length == 0) {
      throw new IllegalArgumentException();
    }

    var cmd = words[0].toLowerCase();
    var params = Arrays.asList(Arrays.copyOfRange(words, 1, words.length));

    switch (cmd) {
      case "echo":
        echo(params);
        break;

      case "time":
        time(params);
        break;

      case "exit":
        isExit = true;
        break;

      case "upload":
        log.info("[EXECUTE CMD] Starting to upload file...");
        upload(params);
        break;

      case "download":
        download(params);
        break;

      default:
    }
  }

  @Override
  public void upload(List<String> params) {
    if (params.size() < 2) {
      throw new IllegalArgumentException("Filename and size params is required.");
    }
    var filename = params.get(0);
    log.info("file size: {}", params.get(1));
    var fileSize = Integer.parseInt(params.get(1));
    receiveFile(filename, fileSize);
  }

  @Override
  public void download(List<String> params) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void time(List<String> params) {
    try {
      sendString(LocalDateTime.now().toString());
    } catch (IOException e) {
      e.printStackTrace();
      isExit = true;
    }
  }

  @Override
  public void exit(List<String> params) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void echo(List<String> params) {
    var strToSend = StringUtils.join(params, " ");
    try {
      sendString(strToSend);
    } catch (IOException e) {
      e.printStackTrace();
      isExit = true;
    }
  }

  @Override
  public void sendFile(File file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void receiveFile(String filename, long fileSize) {
    long offset = 0;
    var file = new File("new_" + filename);
    try {
      if (uploadedOffset == 0) {
        file.delete();
        if (!file.createNewFile()) {
          throw new IOException("Cannot create new file");
        }
      } else if (!file.exists()) {
        throw new IOException("Cannot create new file");
      } else {
        offset = uploadedOffset;
        log.info("[FILE] offset: {}", offset);
      }

      var ok = sendWithReceive(Long.toString(offset), 3);
      if (!"ok".equals(ok)) {
        isExit = true;
        return;
      }

      try (var fileOutputStream = new FileOutputStream(file, true)) {
        byte[] buffer = new byte[bufferSize];

        log.info("[FILE] Start to receive bytes...");
        var receivedBytes = 0;
        var totalReceivedBytes = 0;
        // var oneIterStartTime = System.nanoTime();
        var startTime = System.nanoTime();
        var iterOut = 10;

        while (true) {
          DatagramPacket packet = new DatagramPacket(buffer, bufferSize);
          try {
            socket.receive(packet);
          } catch (SocketTimeoutException e) {
            log.warn("[FILE] Timeout exception");
            break;
          }

          receivedBytes = packet.getLength();
          totalReceivedBytes += packet.getLength();
          offset += receivedBytes;
          fileOutputStream.write(buffer, 0, receivedBytes);
          if (offset >= fileSize) {
            var endTime = System.nanoTime();
            log.info("Final bitrate {} Mbit/s", String.format("%.8f", calcBitrate(totalReceivedBytes, (endTime - startTime)) / 1000000.0));
            log.info("Bytes received: {}", totalReceivedBytes);
            break;
          }
        }

        if (offset < fileSize) {
          log.warn("[FILE] File in not fully downloaded.");
          log.info("Bytes received: {}", totalReceivedBytes);
          this.uploadedOffset = offset;
          isExit = true;
          return;
        } else {
          this.uploadedOffset = 0;
        }

        log.info("[FILE] End to receive bytes.");
      }

    } catch (IOException e) {
      log.error(e.getMessage());
      uploadedOffset = offset;
    }
  }
}
