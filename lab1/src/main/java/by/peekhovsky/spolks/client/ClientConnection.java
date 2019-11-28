package by.peekhovsky.spolks.client;

import by.peekhovsky.spolks.BaseTcpConnection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * @author Rastsislau Piakhouski 2019
 */
@Slf4j
@SuppressWarnings("WeakerAccess")
public class ClientConnection extends BaseTcpConnection {

  public ClientConnection(Socket socket) throws IOException {
    super(socket, LocalTime.of(0, 0, 20).toNanoOfDay(), 1024);
  }

  public void session() throws IOException {
    try (var scanner = new Scanner(System.in)) {

      while (!isExit) {
        String input = scanner.nextLine();
        execute(input);
      }
    }
  }

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
        exit(params);
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

  private void upload(List<String> params) {
    if (params.isEmpty()) {
      throw new IllegalArgumentException("Filename param is required.");
    }
    var filePath = params.get(0);
    log.info("File path: {}", filePath);
    var file = new File(filePath);
    if (!file.exists()) {
      log.warn("File does not exist.");
      return;
    }
    sendFile(file);
  }

  private void download(List<String> params) {
    if (params.isEmpty()) {
      throw new IllegalArgumentException("Filename param is required.");
    }
    try {
      var filename = params.get(0);
      log.info("File path: {}", filename);
      String arg = "download " + filename;
      sendString(arg);
      var message = receiveString();
      if (!message.isEmpty()) {
        log.warn(message);
        return;
      }
      var size = Long.parseLong(receiveString());
      receiveFile(filename, size);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void time(List<String> params) {
    try {
      sendString("time");
      var resp = receiveString();
      log.info("TIME: {}", resp);
    } catch (IOException e) {
      e.printStackTrace();
      isExit = true;
    }
  }

  private void exit(List<String> params) {
    try {
      sendString("exit");
      isExit = true;
    } catch (IOException e) {
      e.printStackTrace();
      isExit = true;
    }
  }

  public void echo(List<String> params) {
    var strToSend = "echo " + StringUtils.join(params, " ");
    try {
      sendString(strToSend);
      var resp = receiveString();
      log.info("ECHO: {}", resp);
    } catch (IOException e) {
      e.printStackTrace();
      isExit = true;
    }
  }

  public void sendFile(File file) {
    log.info("Sending file...");
    try {
      String arg = "upload " + file.getName() + " " + file.length() + "\n";
      log.info("Args: {}", arg);

      out.write(arg.getBytes());

      var offsetStr = receiveString();

      if (offsetStr.isEmpty()) {
        return;
      }

      var offset = Long.parseLong(offsetStr);
      log.info("[FILE] offset: {}", offset);

      try (var fileInputStream = new FileInputStream(file)) {
        log.info("[FILE] Start to send bytes...");
        byte[] buffer = new byte[bufferSize];

        var skippedNumOfBytes = fileInputStream.skip(offset);
        if (skippedNumOfBytes != offset) {
          log.warn("[FILE] skippedNumOfBytes != offset");
        }

        int readBytesFromFile = 0;
        while ((readBytesFromFile = fileInputStream.read(buffer, 0, bufferSize)) != -1) {
          Thread.sleep(200);
          out.write(buffer, 0, readBytesFromFile);
          System.out.print(".");
        }
        System.out.print("\n");
        log.info("[FILE] End to send bytes.");

      } catch (IOException e) {
        log.warn(e.getMessage());
        isExit = true;
      }

    } catch (Exception e) {
      e.printStackTrace();
      isExit = true;
    }
  }

  public void receiveFile(String filename, long fileSize) {
    long offset = 0;
    var file = new File("new1_" + filename);
    try {
      if (!file.exists()) {
        if (!file.createNewFile()) {
          throw new IOException("Cannot create new file");
        }
      } else {
        offset = file.length();
        log.info("[FILE] offset: {}", offset);
      }

      out.write((offset + "\n").getBytes());

      try (var fileOutputStream = new FileOutputStream(file, true)) {
        byte[] buffer = new byte[bufferSize];

        log.info("[FILE] Start to receive bytes...");

        var receivedBytes = 0;
        var totalReceivedBytes = 0;
        // var oneIterStartTime = System.nanoTime();
        var startTime = System.nanoTime();

        while ((receivedBytes = in.read(buffer, 0, bufferSize)) != -1) {
          var endTime = System.nanoTime();

          totalReceivedBytes += receivedBytes;
          offset += receivedBytes;
          fileOutputStream.write(buffer, 0, receivedBytes);

          if (offset >= fileSize) {
            log.info("Final bitrate {} Mbit/s", String.format("%.8f", calcBitrate(totalReceivedBytes, (endTime - startTime)) / 1000000.0));
            log.info("Bytes received: {}", totalReceivedBytes);
            break;
          }

          // log.info("Bitrate {} Mbit/s", String.format("%.8f", calcBitrate(receivedBytes, (endTime - oneIterStartTime)) / 1000000.0));
          // oneIterStartTime = System.nanoTime();
        }

        if (offset < fileSize) {
          log.warn("[FILE] File in not fully downloaded.");
          log.info("Bytes received: {}", totalReceivedBytes);
          return;
        }

        log.info("[FILE] End to receive bytes.");
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
