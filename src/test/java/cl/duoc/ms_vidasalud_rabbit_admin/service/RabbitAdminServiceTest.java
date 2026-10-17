package cl.duoc.ms_vidasalud_rabbit_admin.service;

import cl.duoc.ms_vidasalud_rabbit_admin.config.RabbitAdminProperties;
import cl.duoc.ms_vidasalud_rabbit_admin.dto.CrearColaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test unitario sin contexto de Spring: AmqpAdmin y RabbitTemplate son mocks.
 */
@ExtendWith(MockitoExtension.class)
class RabbitAdminServiceTest {

    @Mock
    private AmqpAdmin amqpAdmin;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        RabbitAdminProperties properties = new RabbitAdminProperties(List.of(
                "q.cmd.email", "q.cmd.admission", "q.cmd.record",
                "q.cmd.email.dlq", "q.cmd.admission.dlq", "q.cmd.record.dlq",
                "cmd.direct", "cmd.topic", "cmd.fanout", "cmd.dead.dlx"));
        ReflectionTestUtils.setField(service, "properties", properties);
    }

    @Test
    void crearColaDeclaraColaDurable() {
        service.crearCola(new CrearColaRequest("q.test.demo"));

        ArgumentCaptor<Queue> cola = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(cola.capture());
        assertEquals("q.test.demo", cola.getValue().getName());
        assertTrue(cola.getValue().isDurable());
    }

    @Test
    void crearColaExistenteResponde409() {
        when(amqpAdmin.getQueueInfo("q.test.demo"))
                .thenReturn(new QueueInformation("q.test.demo", 0, 0));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crearCola(new CrearColaRequest("q.test.demo")));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(amqpAdmin, never()).declareQueue(any());
    }

    @Test
    void crearColaConPrefijoAmqResponde400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crearCola(new CrearColaRequest("amq.prueba")));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(amqpAdmin, never()).declareQueue(any());
    }

    @Test
    void eliminarColaProtegidaResponde409() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.eliminarCola("q.cmd.email"));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void eliminarColaInexistenteResponde404() {
        when(amqpAdmin.getQueueInfo("q.no.existe")).thenReturn(null);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.eliminarCola("q.no.existe"));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void eliminarBindingDelSistemaResponde409() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.eliminarBinding("cmd.topic", "q.cmd.email", "email.*"));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(amqpAdmin, never()).removeBinding(any());
    }
}
