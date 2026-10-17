package cl.duoc.ms_vidasalud_rabbit_admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Nombres de colas y exchanges del sistema que esta API no permite eliminar
 * (propiedad vidasalud.rabbit.protegidas en application.yaml).
 */
@ConfigurationProperties(prefix = "vidasalud.rabbit")
public record RabbitAdminProperties(List<String> protegidas) {
}
