package cl.duoc.ms_vidasalud_rabbit_admin.controller;

import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Errores que ResponseStatusException no cubre: los fallos de @Valid, el JSON ilegible y las
 * excepciones de RabbitMQ que el servicio deja propagar.
 *
 * Se devuelve ProblemDetail (RFC 9457) para que la forma del JSON sea idéntica
 * a la de los errores que Spring genera automáticamente, de modo que el cliente
 * parsea todos los errores igual. Requiere spring.mvc.problemdetails.enabled=true
 * en application.yaml.
 *
 * Esa propiedad registra el ProblemDetailsExceptionHandler de Spring Boot con @Order(0), que
 * también maneja MethodArgumentNotValidException y HttpMessageNotReadableException. Sin
 * HIGHEST_PRECEDENCE, ese handler respondería primero y nunca se agregaría "errores".
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail manejarValidacion(MethodArgumentNotValidException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "La petición tiene campos inválidos");

        // Se reportan TODOS los campos que fallaron, no solo el primero,
        // para que el cliente corrija el formulario en un solo viaje.
        Map<String, String> errores = new HashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
        problema.setProperty("errores", errores);

        return problema;
    }

    /**
     * Un valor de enum desconocido (por ejemplo "tipo": "XYZ") se reporta como error del campo,
     * con los valores permitidos. Cualquier otro problema de lectura es JSON inválido.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail manejarCuerpoIlegible(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof InvalidFormatException invalido
                && invalido.getTargetType() != null
                && invalido.getTargetType().isEnum()
                && !invalido.getPath().isEmpty()) {

            ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST, "La petición tiene campos inválidos");
            problema.setProperty("errores", Map.of(
                    nombreCampo(invalido.getPath()),
                    "Valor inválido. Valores permitidos: " + valoresPermitidos(invalido.getTargetType())));
            return problema;
        }

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido");
    }

    @ExceptionHandler(AmqpConnectException.class)
    public ProblemDetail manejarRabbitCaido(AmqpConnectException e) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "RabbitMQ no está disponible.");
    }

    @ExceptionHandler(AmqpException.class)
    public ProblemDetail manejarRabbitRechazo(AmqpException e) {
        // El motivo real (por ejemplo "PRECONDITION_FAILED - inequivalent arg ...") viene en la
        // causa más interna; el mensaje de AmqpIOException solo dice "java.io.IOException".
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "RabbitMQ rechazó la operación: " + NestedExceptionUtils.getMostSpecificCause(e).getMessage());
    }

    private String nombreCampo(List<JacksonException.Reference> ruta) {
        return ruta.get(ruta.size() - 1).getPropertyName();
    }

    private String valoresPermitidos(Class<?> tipoEnum) {
        return Arrays.stream(tipoEnum.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }
}
