package com.example.otpsentinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Scheduling drives one job only: appending the current minute of operational telemetry. */
@EnableScheduling
@SpringBootApplication
public class OtpSentinelApplication {

  public static void main(String[] args) {
    SpringApplication.run(OtpSentinelApplication.class, args);
  }
}
