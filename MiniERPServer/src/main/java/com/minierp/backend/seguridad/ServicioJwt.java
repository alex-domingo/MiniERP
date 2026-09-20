package com.minierp.backend.seguridad;

import com.minierp.backend.config.PropiedadesSeguridad;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Emision y validacion de JSON Web Tokens.
 *
 * La autenticacion es SIN ESTADO: el servidor no guarda sesiones. Cada
 * peticion trae su token y este servicio lo verifica. Es lo que permite
 * que cliente y servidor sean aplicaciones realmente independientes,
 * como exige la arquitectura del enunciado.
 */
@Service
public class ServicioJwt {

    private static final Logger log = LoggerFactory.getLogger(ServicioJwt.class);

    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_ID = "idUsuario";

    private final SecretKey clave;
    private final long expiracionMs;
    private final String emisor;

    public ServicioJwt(PropiedadesSeguridad propiedades) {
        byte[] bytes = Decoders.BASE64.decode(propiedades.getSecreto());
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "La clave de firma debe tener al menos 32 bytes decodificados "
                            + "para HMAC-SHA256. Revise minierp.seguridad.jwt.secreto");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.expiracionMs = propiedades.getExpiracion();
        this.emisor = propiedades.getEmisor();
    }

    /** Emite un token para el usuario indicado. */
    public String generarToken(String nombreUsuario, Long idUsuario, String rol) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plusMillis(expiracionMs);

        return Jwts.builder()
                .subject(nombreUsuario)
                .claim(CLAIM_ID, idUsuario)
                .claim(CLAIM_ROL, rol)
                .issuer(emisor)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(clave)
                .compact();
    }

    public LocalDateTime calcularExpiracion() {
        return LocalDateTime.ofInstant(
                Instant.now().plusMillis(expiracionMs), ZoneId.systemDefault());
    }

    /**
     * Verifica firma, emisor y vigencia. Devuelve null si el token no
     * es valido por cualquier motivo: un token invalido y uno ausente
     * deben tratarse igual, sin revelar cual de los dos fue.
     */
    public Claims validarYExtraer(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(clave)
                    .requireIssuer(emisor)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token rechazado: {}", ex.getMessage());
            return null;
        }
    }

    public String extraerUsuario(Claims claims) {
        return claims.getSubject();
    }

    public String extraerRol(Claims claims) {
        return claims.get(CLAIM_ROL, String.class);
    }

    public Long extraerIdUsuario(Claims claims) {
        Number valor = claims.get(CLAIM_ID, Number.class);
        return valor == null ? null : valor.longValue();
    }
}
