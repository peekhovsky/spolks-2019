package by.peekhovsky.spolks;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.util.List;

/**
 * @author Rastsislau Piakhouski 2019
 */
public interface Connection {
  void upload(List<String> params);

  void time(List<String> params);

  void exit(List<String> params);

  void echo(List<String> params);

  void sendFile(File file);

  void receiveFile(String filename, long fileSize);

  void changeSocket(DatagramSocket newSocket) throws IOException;

  void sendString(String str) throws IOException;

  String receiveString() throws IOException;

  void execute(String cmdWithParams);

  void download(List<String> params);

  void setExit(boolean exit);
}
