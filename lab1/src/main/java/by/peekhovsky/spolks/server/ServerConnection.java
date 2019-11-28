package by.peekhovsky.spolks.server;

import by.peekhovsky.spolks.BaseTcpConnection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * @author Rastsislau Piakhouski 2019
 */
@Slf4j
@SuppressWarnings("WeakerAccess")
public class ServerConnection extends BaseTcpConnection {

  public ServerConnection(Socket socket) throws IOException {
    super(socket, LocalTime.of(0, 0, 20).toNanoOfDay(), 1024);
  }

  public void changeSocket(Socket newSocket) throws IOException {
    this.socket = newSocket;
    if (this.in != null) {
      in.close();
    }
    if (this.out != null) {
      out.close();
    }
    this.in = newSocket.getInputStream();
    this.out = newSocket.getOutputStream();
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

  private void upload(List<String> params) {
    if (params.size() < 2) {
      throw new IllegalArgumentException("Filename and size params is required.");
    }
    var filename = params.get(0);
    log.info("file size: {}", params.get(1));
    var fileSize = Integer.parseInt(params.get(1));
    receiveFile(filename, fileSize);
  }

  private void download(List<String> params) {
    if (params.isEmpty()) {
      throw new IllegalArgumentException("Filename param is required.");
    }
    var filePath = params.get(0);
    log.info("File path: {}", filePath);
    var file = new File(filePath);
    if (!file.exists()) {
      log.warn("File does not exist.");
      try {
        sendString("File does not exist.");
      } catch (IOException e) {
        e.printStackTrace();
        isExit = true;
      }
      return;
    }
    sendFile(file);
  }

  private void time(List<String> params) {
    try {
      sendString(LocalDateTime.now().toString());
    } catch (IOException e) {
      e.printStackTrace();
      isExit = true;
    }
  }

  public void echo(List<String> params) {
    var strToSend = StringUtils.join(params, " ");
    try {
      sendString(strToSend);
    } catch (IOException e) {
      e.printStackTrace();
      isExit = true;
    }
  }

  public void sendFile(File file) {
    log.info("Sending file...");
    try {
      sendString("");
      sendString(Long.toString(file.length()));
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

  public void receiveFile(String filename, int fileSize) {
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
          this.uploadedOffset = offset;
          isExit = true;
          return;
        } else {
          this.uploadedOffset = 0;
        }

        log.info("[FILE] End to receive bytes.");
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
