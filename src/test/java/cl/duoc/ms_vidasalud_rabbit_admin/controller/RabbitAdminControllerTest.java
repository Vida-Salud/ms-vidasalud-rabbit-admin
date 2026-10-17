package cl.duoc.ms_vidasalud_rabbit_admin.controller;

import cl.duoc.ms_vidasalud_rabbit_admin.dto.CrearColaRequest;
import cl.duoc.ms_vidasalud_rabbit_admin.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RabbitAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RabbitAdminService rabbitAdminService;

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(post("/api/rabbit/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"q.test.demo\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/rabbit/queues/q.test.demo"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/rabbit/exchanges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"ex.x\", \"tipo\": \"TOPIC\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/rabbit/exchanges/ex.x"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/rabbit/bindings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exchange\": \"ex.x\", \"cola\": \"q.test.demo\", \"routingKey\": \"\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/rabbit/bindings")
                        .param("exchange", "ex.x")
                        .param("cola", "q.test.demo"))
                .andExpect(status().isUnauthorized());

        verify(rabbitAdminService, never()).crearCola(any());
    }

    @Test
    void crearColaConNombreVacioResponde400() throws Exception {
        mockMvc.perform(post("/api/rabbit/queues")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());

        verify(rabbitAdminService, never()).crearCola(any());
    }

    @Test
    void crearExchangeConTipoInvalidoResponde400() throws Exception {
        mockMvc.perform(post("/api/rabbit/exchanges")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"ex.x\", \"tipo\": \"XYZ\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tipo")
                        .value("Valor inválido. Valores permitidos: DIRECT, TOPIC, FANOUT"));

        verify(rabbitAdminService, never()).crearExchange(any());
    }

    @Test
    void crearColaValidaResponde201() throws Exception {
        mockMvc.perform(post("/api/rabbit/queues")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"q.test.demo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("q.test.demo"));

        verify(rabbitAdminService).crearCola(new CrearColaRequest("q.test.demo"));
    }
}
