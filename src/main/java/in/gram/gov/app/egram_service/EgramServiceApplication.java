package in.gram.gov.app.egram_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class EgramServiceApplication {

     static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));  // <--- fix
        SpringApplication.run(EgramServiceApplication.class, args);
    }
}
