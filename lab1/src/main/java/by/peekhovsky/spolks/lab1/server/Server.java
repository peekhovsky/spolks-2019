package by.peekhovsky.spolks.lab1.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Server {

  @Value("#{new Integer('${server.port}')}")
  private Integer port;

  @Value("server.host")
  private String host;

  public void start() {
    log.info("Port: {}", port);
    log.info("Host: {}", host);
  }
}
