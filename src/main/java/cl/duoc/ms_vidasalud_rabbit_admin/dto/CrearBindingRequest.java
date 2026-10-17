package cl.duoc.ms_vidasalud_rabbit_admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;


public record CrearBindingRequest(

        @NotBlank(message = "El exchange es obligatorio")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{1,200}$",
                message = "El exchange solo admite letras, números, punto, guion y guion bajo (máximo 200 caracteres)")
        String exchange,

        @NotBlank(message = "La cola es obligatoria")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{1,200}$",
                message = "La cola solo admite letras, números, punto, guion y guion bajo (máximo 200 caracteres)")
        String cola,

        /** Puede ser "" (los exchanges fanout ignoran la routing key). */
        @NotNull(message = "La routing key es obligatoria (puede ser vacía)")
        @Pattern(regexp = "^[a-zA-Z0-9._*#-]{0,200}$",
                message = "La routing key solo admite letras, números, punto, guion, guion bajo, * y # (máximo 200 caracteres)")
        String routingKey
) {
}
