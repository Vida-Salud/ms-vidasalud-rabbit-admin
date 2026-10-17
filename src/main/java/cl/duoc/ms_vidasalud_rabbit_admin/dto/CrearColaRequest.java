package cl.duoc.ms_vidasalud_rabbit_admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


public record CrearColaRequest(

        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{1,200}$",
                message = "El nombre solo admite letras, números, punto, guion y guion bajo (máximo 200 caracteres)")
        String nombre
) {
}
