package by.peekhovsky.spolks.client;

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
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * @author Rastsislau Piakhouski 2019
 */
@Slf4j
@SuppressWarnings("WeakerAccess")
public class ClientConnection extends BaseUdpConnection {


  public ClientConnection(DatagramSocket socket, InetAddress inetAddress, int port) throws IOException {
    super(socket, 5000, 1024, inetAddress, port);
  }

  public void session() throws IOException {
    try (var scanner = new Scanner(System.in)) {

      while (!isExit) {
        String input = scanner.nextLine();
        execute(input);
      }
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
        exit(params);
        break;

      case "upload":
        log.info("[EXECUTE CMD] Starting to upload file...");
        upload(params);
        break;

      case "download":
        //download(params);
        break;

      default:
    }
  }

  @Override
  public void download(List<String> params) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void upload(List<String> params) {
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

  @Override
  public void time(List<String> params) {
    try {
      sendString("time");
      var resp = receiveString();
      log.info("TIME: {}", resp);
    } catch (IOException e) {
      e.printStackTrace();
      isExit = true;
    }
  }

  @Override
  public void exit(List<String> params) {
    try {
      sendString("exit");
      isExit = true;
    } catch (IOException e) {
      e.printStackTrace();
      isExit = true;
    }
  }

  @Override
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

  @Override
  public void sendFile(File file) {
    log.info("Sending file...");
    try {
      String arg = "upload " + file.getName() + " " + file.length();
      log.info("Args: {}", arg);

      var offsetStr = sendWithReceive(arg, 3);
      if (offsetStr.isEmpty()) {
        return;
      } else {
        sendString("ok");
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
          DatagramPacket packet = new DatagramPacket(buffer, readBytesFromFile, inetAddress, port);
          socket.send(packet);

          System.out.print(".");
        }

        sendString("END_OF_FILE");
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

  @Override
  public void receiveFile(String filename, long fileSize) {
    throw new UnsupportedOperationException();
  }
}
