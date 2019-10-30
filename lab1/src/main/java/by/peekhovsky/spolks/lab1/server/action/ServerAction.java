package by.peekhovsky.spolks.lab1.server.action;

import java.net.Socket;

/**
 * @author Rastsislau Piakhouski 2019
 */
public interface ServerAction {
  void execute(Socket socket);
}
