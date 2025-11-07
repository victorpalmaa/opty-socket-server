/**
 * Main entry point for Opty Socket Server.
 * 
 * @author Opty Development Team
 * @version 1.0.0
 */

package com.opty.socket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * CODE
 */


@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableMongoRepositories(basePackages = "com.opty.socket.repository")
public class OptiSocketApplication {

    public static void main(String[] args) {
        log.info("Starting Opty Socket Server...");
        SpringApplication.run(OptiSocketApplication.class, args);
        log.info("Opty Socket Server started successfully!");
    }
}
