package by.peekhovsky.spolks.server.session;

import by.peekhovsky.spolks.server.ServerConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Rastsislau Piakhouski 2019
 */
@Service
@Slf4j
public class ConnectionLoader {

  /**
   * Map<IP of a socket, Session>
   */
  private Map<String, ServerConnection> connections = new ConcurrentHashMap<>();

  /**
   * Loads old session by sessionId or creates new one.
   * @param socket client socket
   */
  public ServerConnection loadConnection(@NonNull Socket socket) throws IOException {
    log.info("Loading connection, ip: {}", socket.getLocalAddress());
    ServerConnection connection = connections.get(socket.getLocalAddress().toString());
    if (connection == null) {
      log.info("Connection is not found, try to create new one...");
      connection = new ServerConnection(socket);
      log.info("Connection has been created.");
      connections.put(socket.getLocalAddress().toString(), connection);
    } else {
      connection.setExit(false);
      connection.changeSocket(socket);
    }
    return connection;
  }

  public void closeConnection(@NonNull String sessionId) {
    connections.remove(sessionId);
  }
}
