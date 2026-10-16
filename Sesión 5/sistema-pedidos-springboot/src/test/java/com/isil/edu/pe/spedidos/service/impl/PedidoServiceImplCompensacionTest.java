package com.isil.edu.pe.spedidos.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.isil.edu.pe.spedidos.client.DevolucionStockClient;
import com.isil.edu.pe.spedidos.client.InventarioClient;
import com.isil.edu.pe.spedidos.client.dto.ReservaStockResponse;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.repository.PedidoRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PedidoServiceImplCompensacionTest {

    @Test
    void debeCompensarLaReservaSiFallaElGuardadoLocal() {
        // Mocks
        PedidoRepository pedidoRepository = mock(PedidoRepository.class);
        InventarioClient inventarioClient = mock(InventarioClient.class);
        DevolucionStockClient devolucionStockClient = mock(DevolucionStockClient.class);

        // Instancia del servicio bajo prueba
        PedidoServiceImpl service = new PedidoServiceImpl(
                pedidoRepository,
                inventarioClient,
                devolucionStockClient,
                false
        );

        // Configuración del objeto de respuesta simulado
        ReservaStockResponse reserva = new ReservaStockResponse();
        reserva.setCodigo("LAP-001");
        reserva.setNombre("Laptop");
        reserva.setPrecio(new BigDecimal("2500.00"));

        // Comportamiento de los Mocks (When)
        when(inventarioClient.reservarStock("LAP-001", 2)).thenReturn(reserva);
        when(pedidoRepository.save(any(Pedido.class)))
                .thenThrow(new IllegalStateException("Fallo local simulado"));

        // Verificación de la excepción esperada (Assert)
        assertThrows(
                IllegalStateException.class,
                () -> service.registrarPedido("Ana Torres", "LAP-001", 2)
        );

        // Verificación de que el método de compensación fue llamado (Verify)
        verify(devolucionStockClient, times(1)).devolverStock("LAP-001", 2);
    }
}
