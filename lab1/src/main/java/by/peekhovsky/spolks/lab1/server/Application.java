package by.peekhovsky.spolks.lab1.server;

import by.peekhovsky.spolks.lab1.server.config.ApplicationConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


@Slf4j
public class Application {
  public static void main(String[] args) {
    var context = new AnnotationConfigApplicationContext();
    context.register(ApplicationConfig.class);
    context.scan("by.peekhovsky.spolks.lab1");
    context.refresh();

    var server = context.getBean(Server.class);
    server.start();
  }
}
