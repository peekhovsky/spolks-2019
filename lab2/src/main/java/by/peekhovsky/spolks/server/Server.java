package by.peekhovsky.spolks.server;


import by.peekhovsky.spolks.server.session.ConnectionLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;

/**
 * @author Rastsislau Piakhouski 2019
 */
@Component
@Slf4j
@SuppressWarnings("WeakerAccess")
public class Server {

  private ServerSocket serverSocket;

  @Value("${server.host}")
  private String host;

  @Value("#{new Integer('${server.port}')}")
  private Integer port;

  private final ConnectionLoader connectionLoader;

  private boolean isExit = false;


  @Autowired
  public Server(ConnectionLoader connectionLoader) {
    this.connectionLoader = connectionLoader;
  }

  @PostConstruct
  private void init() throws IOException {
    log.info("host: {}", host);
    log.info("port: {}", port);
    serverSocket = new ServerSocket(port);
  }

  public void start() throws IOException {
    log.info("Server has been started");
    while (!isExit) {
      log.info("Pending client...");
      try (DatagramSocket socket = new DatagramSocket(port)) {
        byte[] buff = new byte[10];
        DatagramPacket datagramPacket = new DatagramPacket(buff, 10);
        socket.receive(datagramPacket);
        log.info("Connection has been accepted.");
        ServerConnection connection = connectionLoader.loadConnection(socket, datagramPacket);
        connection.executeSession();
        log.info("Connection has been closed.");
      }
    }
  }

  public void setExit(boolean exit) {
    isExit = exit;
  }
}
