package cl.duoc.ms_vidasalud_rabbit_admin.service;

import cl.duoc.ms_vidasalud_rabbit_admin.dto.CrearBindingRequest;
import cl.duoc.ms_vidasalud_rabbit_admin.dto.CrearColaRequest;
import cl.duoc.ms_vidasalud_rabbit_admin.dto.CrearExchangeRequest;

public interface RabbitAdminService {
    void crearCola(CrearColaRequest request);
    void eliminarCola(String nombre);
    void crearExchange(CrearExchangeRequest request);
    void eliminarExchange(String nombre);
    void crearBinding(CrearBindingRequest request);
    void eliminarBinding(String exchange, String cola, String routingKey);
}
