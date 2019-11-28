package by.peekhovsky.spolks.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 * @author Rastsislau Piakhouski 2019
 */
@Component
public class Client {

  @Value("${server.host}")
  private String host;

  @Value("#{new Integer('${server.port}')}")
  private Integer port;

  @Value("#{new Integer('${server.buffer.size}')}")
  private Integer bufferSize;

  public void start() throws IOException {
    Socket socket = new Socket(host, port);
    ClientConnection clientConnection = new ClientConnection(socket);
    clientConnection.session();
  }
}
