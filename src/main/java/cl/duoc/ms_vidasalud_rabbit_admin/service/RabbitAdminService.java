package cl.duoc.ms_vidasalud_rabbit_admin.service;

import cl.duoc.ms_vidasalud_rabbit_admin.config.RabbitAdminProperties;
import cl.duoc.ms_vidasalud_rabbit_admin.dto.CrearBindingRequest;
import cl.duoc.ms_vidasalud_rabbit_admin.dto.CrearColaRequest;
import cl.duoc.ms_vidasalud_rabbit_admin.dto.CrearExchangeRequest;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ShutdownSignalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Administra colas, exchanges y bindings de RabbitMQ a través de {@link AmqpAdmin}.
 * <p>
 * Cada método valida en este orden: 400 (nombre reservado o vacío), 409 (recurso protegido)
 * y por último 404/409 según exista o no el recurso en el broker. Los recursos protegidos se
 * leen de {@code vidasalud.rabbit.protegidas}. Los errores del broker ({@link AmqpException})
 * se propagan y los traduce {@code ManejadorErrores}.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    /** Prefijo que RabbitMQ reserva para sus propios recursos. */
    private static final String PREFIJO_RESERVADO = "amq.";

    @Autowired
    private AmqpAdmin amqpAdmin;

    /** Solo se usa para la declaración pasiva que verifica si un exchange existe. */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdminProperties properties;

    public void crearCola(CrearColaRequest request) {
        String nombre = request.nombre();
        validarNoReservado(nombre, "cola");
        if (existeCola(nombre)) {
            throw rechazo(HttpStatus.CONFLICT, "La cola " + nombre + " ya existe");
        }

        amqpAdmin.declareQueue(new Queue(nombre, true));
        log.info("Cola creada [nombre={}]", nombre);
    }

    public void eliminarCola(String nombre) {
        validarNoReservado(nombre, "cola");
        if (esProtegido(nombre)) {
            throw rechazo(HttpStatus.CONFLICT, "La cola " + nombre + " es del sistema y no se puede eliminar");
        }
        if (!existeCola(nombre)) {
            throw rechazo(HttpStatus.NOT_FOUND, "No existe una cola con nombre " + nombre);
        }

        amqpAdmin.deleteQueue(nombre);
        log.info("Cola eliminada [nombre={}]", nombre);
    }

    public void crearExchange(CrearExchangeRequest request) {
        String nombre = request.nombre();
        validarNoReservado(nombre, "exchange");
        if (existeExchange(nombre)) {
            throw rechazo(HttpStatus.CONFLICT, "El exchange " + nombre + " ya existe");
        }

        Exchange exchange = switch (request.tipo()) {
            case DIRECT -> new DirectExchange(nombre, true, false);
            case TOPIC -> new TopicExchange(nombre, true, false);
            case FANOUT -> new FanoutExchange(nombre, true, false);
        };
        amqpAdmin.declareExchange(exchange);
        log.info("Exchange creado [nombre={}, tipo={}]", nombre, request.tipo());
    }

    public void eliminarExchange(String nombre) {
        validarNoReservado(nombre, "exchange");
        if (esProtegido(nombre)) {
            throw rechazo(HttpStatus.CONFLICT, "El exchange " + nombre + " es del sistema y no se puede eliminar");
        }
        if (!existeExchange(nombre)) {
            throw rechazo(HttpStatus.NOT_FOUND, "No existe un exchange con nombre " + nombre);
        }

        amqpAdmin.deleteExchange(nombre);
        log.info("Exchange eliminado [nombre={}]", nombre);
    }

    /**
     * Se permite enlazar una cola nueva a un exchange protegido (por ejemplo cmd.topic -> q.sms).
     */
    public void crearBinding(CrearBindingRequest request) {
        validarExistenAmbos(request.exchange(), request.cola());

        amqpAdmin.declareBinding(toBinding(request.exchange(), request.cola(), request.routingKey()));
        log.info("Binding creado [exchange={}, cola={}, routingKey={}]",
                request.exchange(), request.cola(), request.routingKey());
    }

    /**
     * Solo se rechazan los bindings entre una cola y un exchange protegidos: son los que
     * declara ms-vidasalud-notify. Un binding de una cola nueva a cmd.topic sí se puede borrar.
     */
    public void eliminarBinding(String exchange, String cola, String routingKey) {
        if (exchange.isBlank() || cola.isBlank()) {
            throw rechazo(HttpStatus.BAD_REQUEST, "El exchange y la cola son obligatorios");
        }
        if (esProtegido(exchange) && esProtegido(cola)) {
            throw rechazo(HttpStatus.CONFLICT, "El binding " + exchange + " -> " + cola
                    + " es del sistema y no se puede eliminar");
        }
        validarExistenAmbos(exchange, cola);

        amqpAdmin.removeBinding(toBinding(exchange, cola, routingKey));
        log.info("Binding eliminado [exchange={}, cola={}, routingKey={}]", exchange, cola, routingKey);
    }

    private void validarNoReservado(String nombre, String recurso) {
        if (nombre.startsWith(PREFIJO_RESERVADO)) {
            throw rechazo(HttpStatus.BAD_REQUEST, "El nombre de " + recurso + " " + nombre
                    + " usa el prefijo " + PREFIJO_RESERVADO + ", reservado por RabbitMQ");
        }
    }

    private void validarExistenAmbos(String exchange, String cola) {
        if (!existeCola(cola)) {
            throw rechazo(HttpStatus.NOT_FOUND, "No existe una cola con nombre " + cola);
        }
        if (!existeExchange(exchange)) {
            throw rechazo(HttpStatus.NOT_FOUND, "No existe un exchange con nombre " + exchange);
        }
    }

    private boolean esProtegido(String nombre) {
        return properties.protegidas().contains(nombre);
    }

    private boolean existeCola(String nombre) {
        return amqpAdmin.getQueueInfo(nombre) != null;
    }

    /**
     * Declaración pasiva: RabbitMQ responde NOT_FOUND (404) si el exchange no existe. Cualquier
     * otro error (por ejemplo, broker caído) se propaga.
     */
    private boolean existeExchange(String nombre) {
        try {
            rabbitTemplate.execute(channel -> channel.exchangeDeclarePassive(nombre));
            return true;
        } catch (AmqpException e) {
            if (esNotFound(e)) {
                return false;
            }
            throw e;
        }
    }

    /** El cierre de canal con código 404 llega envuelto en la cadena de causas de la excepción. */
    private boolean esNotFound(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof ShutdownSignalException senal
                    && senal.getReason() instanceof AMQP.Channel.Close cierre
                    && cierre.getReplyCode() == AMQP.NOT_FOUND) {
                return true;
            }
        }
        return false;
    }

    private Binding toBinding(String exchange, String cola, String routingKey) {
        return new Binding(cola, Binding.DestinationType.QUEUE, exchange, routingKey, null);
    }

    private ResponseStatusException rechazo(HttpStatus estado, String motivo) {
        log.warn("Operación rechazada [{}]: {}", estado.value(), motivo);
        return new ResponseStatusException(estado, motivo);
    }
}
