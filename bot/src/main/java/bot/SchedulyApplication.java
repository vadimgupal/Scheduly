package bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({BotConfig.class})
public class SchedulyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulyApplication.class, args);
    }

}
