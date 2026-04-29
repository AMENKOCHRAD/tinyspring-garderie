package com.tinyspring.garderie.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
<<<<<<< HEAD
//configurer comment les objets Java sont transformés en JSON
=======

>>>>>>> origin/gestion-evenements
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
<<<<<<< HEAD
        return new ObjectMapper().findAndRegisterModules();
    }
}

=======
        return new ObjectMapper();
    }
}
>>>>>>> origin/gestion-evenements
