package by.peekhovsky.spolks.lab1.server.connection;

import java.net.Socket;

/**
 * @author Rastsislau Piakhouski 2019
 */
public interface Connector {
  void holdConnection(Socket socket);
}
