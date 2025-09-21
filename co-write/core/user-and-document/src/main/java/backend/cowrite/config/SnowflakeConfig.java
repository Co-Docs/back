package backend.cowrite.config;

import kuke.board.common.snowflake.Snowflake;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnowflakeConfig {

    @Bean
    public Snowflake snowFlake(){
        return new Snowflake();
    }
}
