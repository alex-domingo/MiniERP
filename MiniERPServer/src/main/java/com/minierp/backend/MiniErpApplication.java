package com.minierp.backend;

import com.minierp.backend.config.PropiedadesEmpresa;
import com.minierp.backend.config.PropiedadesNegocio;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Punto de entrada del servidor Mini ERP.
 *
 * El esquema de la base lo gobierna Flyway; Hibernate solo lo valida
 * al arrancar (spring.jpa.hibernate.ddl-auto=validate). Si una entidad
 * no concuerda con su tabla, la aplicacion falla aqui mismo en lugar
 * de alterar la base en caliente.
 */
@SpringBootApplication
@EnableConfigurationProperties({PropiedadesNegocio.class, PropiedadesEmpresa.class})
public class MiniErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniErpApplication.class, args);
    }
}
