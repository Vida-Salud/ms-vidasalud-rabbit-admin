package cl.duoc.ms_vidasalud_rabbit_admin.controller;

import cl.duoc.ms_vidasalud_rabbit_admin.dto.CrearBindingRequest;
import cl.duoc.ms_vidasalud_rabbit_admin.dto.CrearColaRequest;
import cl.duoc.ms_vidasalud_rabbit_admin.dto.CrearExchangeRequest;
import cl.duoc.ms_vidasalud_rabbit_admin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST para crear y eliminar colas, exchanges y bindings de RabbitMQ.
 * <p>
 * Solo valida el formato de la petición y delega en {@link RabbitAdminService}. Los POST
 * responden 201 con el mismo request como body; los DELETE responden 204 sin body.
 */
@RestController
@RequestMapping("/api/rabbit")
public class RabbitAdminController {

    @Autowired
    private RabbitAdminService rabbitAdminService;

    @PostMapping("/queues")
    public ResponseEntity<CrearColaRequest> crearCola(@Valid @RequestBody CrearColaRequest request) {
        rabbitAdminService.crearCola(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> eliminarCola(@PathVariable String name) {
        rabbitAdminService.eliminarCola(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<CrearExchangeRequest> crearExchange(@Valid @RequestBody CrearExchangeRequest request) {
        rabbitAdminService.crearExchange(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String name) {
        rabbitAdminService.eliminarExchange(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    public ResponseEntity<CrearBindingRequest> crearBinding(@Valid @RequestBody CrearBindingRequest request) {
        rabbitAdminService.crearBinding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    /** routingKey es opcional: los bindings a exchanges fanout usan "". */
    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(
            @RequestParam String exchange,
            @RequestParam String cola,
            @RequestParam(defaultValue = "") String routingKey) {

        rabbitAdminService.eliminarBinding(exchange, cola, routingKey);
        return ResponseEntity.noContent().build();
    }
}
