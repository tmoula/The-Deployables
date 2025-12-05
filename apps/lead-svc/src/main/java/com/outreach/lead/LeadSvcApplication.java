// The on switch for the app
package com.outreach.lead;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRabbit  // Enable RabbitMQ listeners
public class LeadSvcApplication {
  public static void main(String[] args) {
    SpringApplication.run(LeadSvcApplication.class, args);
  }
}

