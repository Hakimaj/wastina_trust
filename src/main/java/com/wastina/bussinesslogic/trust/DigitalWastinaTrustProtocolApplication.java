package com.wastina.bussinesslogic.trust;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main application class for Digital Wastina Trust Protocol
 * 
 * @author Ephrem Tesfaye, Abdulihakim Jejaw, Rihad Gali
 */
@SpringBootApplication
@EnableJpaAuditing
public class DigitalWastinaTrustProtocolApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalWastinaTrustProtocolApplication.class, args);
    }
}
