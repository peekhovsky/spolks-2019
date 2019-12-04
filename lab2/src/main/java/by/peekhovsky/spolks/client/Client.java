package by.peekhovsky.spolks.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * @author Rastsislau Piakhouski 2019
 */
@Component
public class Client {

  @Value("${server.host}")
  private String host;

  @Value("#{new Integer('${server.port}')}")
  private Integer port;

  public void start() throws IOException {
    DatagramSocket socket = new DatagramSocket();
    InetAddress address = InetAddress.getByName("localhost");
    socket.connect(address, port);
    var bytes = "CONNECT".getBytes();
    DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
    socket.send(packet);
    ClientConnection clientConnection = new ClientConnection(socket, address, port);
    clientConnection.session();
  }
}
