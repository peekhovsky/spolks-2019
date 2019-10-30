package by.peekhovsky.spolks.lab1.server.connection;

import com.google.common.base.Enums;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.EnumSet;


@Component
@Slf4j
public class Server {
  private ServerSocket serverSocket;
  private Socket clientSocket;
  private PrintWriter out;
  private BufferedReader in;

  public void start(int port) throws IOException {
    serverSocket = new ServerSocket(port);

    while (true) {
      clientSocket = serverSocket.accept();
      out = new PrintWriter(clientSocket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      String inputStr = in.readLine();

      var command = new ParametrizedCommand(inputStr);
      evaluate(command);

      break;
    }
  }

  private void evaluate(ParametrizedCommand command) {

  }

  public void stop() throws IOException {
    in.close();
    out.close();
    clientSocket.close();
    serverSocket.close();
  }


  public static class ParametrizedCommand {
    @Getter
    private ServerCommand command;

    @Getter
    private String[] params;

    public ParametrizedCommand(@NonNull String inputSrt) {
      init(inputSrt);
    }

    void init(String inputSrt) {
      String[] splitStr = inputSrt.split(" ");
      String commandStr = splitStr[0];
      if (commandStr == null) {
        commandStr = inputSrt;
        this.params = new String[0];
      } else {
        this.params = ArrayUtils.remove(splitStr, 0);
      }

      commandStr = commandStr.toUpperCase();
      this.command = Enums.getIfPresent(ServerCommand.class, commandStr).or(ServerCommand.UNKNOWN);
    }
  }

  public enum ServerCommand {
    ECHO,
    TIME,
    CLOSE,
    UPLOAD_FILE,
    UNKNOWN
  }
}