package by.peekhovsky.spolks.client;


import by.peekhovsky.spolks.client.config.ClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;


@Slf4j
public class ClientRunner {
  public static void main(String[] args) throws IOException {
    var context = new AnnotationConfigApplicationContext();
    context.register(ClientConfig.class);
    context.scan("by.peekhovsky.spolks.client");
    context.refresh();
    var client = context.getBean(Client.class);
    client.start();
  }
}
