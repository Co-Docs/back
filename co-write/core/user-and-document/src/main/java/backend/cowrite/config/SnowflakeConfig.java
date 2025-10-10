package backend.cowrite.config;

import backend.cowrite.common.snowflake.Snowflake;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnowflakeConfig {

    @Bean
    public Snowflake snowFlake(){
        return new Snowflake();
    }
}
