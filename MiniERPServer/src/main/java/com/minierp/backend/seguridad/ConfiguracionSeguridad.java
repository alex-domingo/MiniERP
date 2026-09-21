package com.minierp.backend.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Control de acceso por roles, que responde al objetivo 7 del
 * enunciado.
 *
 * Los cuatro roles corresponden a las areas funcionales:
 *
 *   ADMINISTRACION  supervisa y consulta todo, pero no opera: no
 *                   registra compras ni ventas
 *   COMPRAS         proveedores y compras
 *   INVENTARIO      catalogo, existencias, kardex y ajustes
 *   VENTAS          clientes y ventas
 *
 * Inventario puede LEER compras y ventas porque necesita rastrear el
 * origen de cada movimiento del kardex.
 *
 * Criterio general: la lectura se reparte segun quien necesita la
 * informacion; la escritura queda restringida al area duena del dato.
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    private static final String ADMIN = "ADMINISTRACION";
    private static final String COMPRAS = "COMPRAS";
    private static final String INVENTARIO = "INVENTARIO";
    private static final String VENTAS = "VENTAS";

    private final FiltroAutenticacionJwt filtroJwt;
    private final ManejadorErroresSeguridad manejadorErrores;
    private final String origenesPermitidos;

    public ConfiguracionSeguridad(FiltroAutenticacionJwt filtroJwt,
                                  ManejadorErroresSeguridad manejadorErrores,
                                  @Value("${minierp.cors.origenes}") String origenesPermitidos) {
        this.filtroJwt = filtroJwt;
        this.manejadorErrores = manejadorErrores;
        this.origenesPermitidos = origenesPermitidos;
    }

    /**
     * BCrypt con factor 10, que es el que uso la carga inicial. Subirlo
     * invalidaria los hash ya almacenados en la migracion V2.
     */
    @Bean
    public PasswordEncoder codificadorContrasena() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager gestorAutenticacion(AuthenticationConfiguration configuracion)
            throws Exception {
        return configuracion.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain cadenaFiltros(HttpSecurity http) throws Exception {
        http
                // Sin CSRF: la API no usa cookies de sesion, la
                // autenticacion viaja en una cabecera que un sitio
                // externo no puede adjuntar automaticamente.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())

                // Sin estado: ninguna peticion depende de una sesion previa.
                .sessionManagement(sesion ->
                        sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(manejo -> manejo
                        .authenticationEntryPoint(manejadorErrores)
                        .accessDeniedHandler(manejadorErrores))

                .authorizeHttpRequests(rutas -> rutas

                        // --- Publico ---
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/salud").permitAll()

                        // --- Exclusivo de Administracion ---
                        .requestMatchers("/api/usuarios/**").hasRole(ADMIN)
                        .requestMatchers("/api/bitacora/**").hasRole(ADMIN)

                        // --- Catalogo: lo mantiene Inventario, lo lee todo el mundo ---
                        .requestMatchers(HttpMethod.GET, "/api/categorias/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/productos/**").authenticated()
                        .requestMatchers("/api/categorias/**").hasRole(INVENTARIO)
                        .requestMatchers("/api/productos/**").hasRole(INVENTARIO)

                        // --- Proveedores y compras: los maneja Compras ---
                        .requestMatchers(HttpMethod.GET, "/api/proveedores/**")
                            .hasAnyRole(ADMIN, COMPRAS, INVENTARIO)
                        .requestMatchers(HttpMethod.GET, "/api/compras/**")
                            .hasAnyRole(ADMIN, COMPRAS, INVENTARIO)
                        .requestMatchers("/api/proveedores/**").hasRole(COMPRAS)
                        .requestMatchers("/api/compras/**").hasRole(COMPRAS)

                        // --- Clientes y ventas: los maneja Ventas ---
                        .requestMatchers(HttpMethod.GET, "/api/clientes/**")
                            .hasAnyRole(ADMIN, VENTAS)
                        .requestMatchers(HttpMethod.GET, "/api/ventas/**")
                            .hasAnyRole(ADMIN, VENTAS, INVENTARIO)
                        .requestMatchers("/api/clientes/**").hasRole(VENTAS)
                        .requestMatchers("/api/ventas/**").hasRole(VENTAS)

                        // --- Inventario: kardex y valuacion exponen COSTOS, asi que
                        //     Ventas no los lee (consulta existencias en /api/productos).
                        //     Solo el area de Inventario registra ajustes. ---
                        .requestMatchers(HttpMethod.GET, "/api/inventario/**")
                            .hasAnyRole(ADMIN, COMPRAS, INVENTARIO)
                        .requestMatchers("/api/inventario/**").hasRole(INVENTARIO)

                        // --- Reportes: el servicio filtra segun el rol ---
                        .requestMatchers("/api/reportes/**").authenticated()

                        .anyRequest().authenticated())

                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS para el cliente Angular. Los origenes se leen de la
     * configuracion en lugar de fijarse aqui, para no tener que
     * recompilar al desplegar en otra direccion.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(
                Arrays.stream(origenesPermitidos.split(",")).map(String::trim).toList());
        configuracion.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuracion.setExposedHeaders(List.of("Content-Disposition"));
        configuracion.setAllowCredentials(true);
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }
}
