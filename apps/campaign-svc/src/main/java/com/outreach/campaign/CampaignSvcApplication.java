// The on switch for the app
package com.outreach.campaign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CampaignSvcApplication {
  public static void main(String[] args) {
    SpringApplication.run(CampaignSvcApplication.class, args);
  }
}

