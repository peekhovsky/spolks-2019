package by.peekhovsky.spolks.server;


import by.peekhovsky.spolks.server.config.ServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;


@Slf4j
public class ServerRunner {
  public static void main(String[] args) throws IOException {
    var context = new AnnotationConfigApplicationContext();
    context.register(ServerConfig.class);
    context.scan("by.peekhovsky.spolks.server");
    context.refresh();
    var server = context.getBean(Server.class);
    server.start();
  }
}
