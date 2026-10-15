package com.isil.edu.pe.spedidos.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.isil.edu.pe.spedidos.client.DevolucionStockClient;
import com.isil.edu.pe.spedidos.client.InventarioNoDisponibleException;
import com.isil.edu.pe.spedidos.client.dto.DevolucionStockResponse;
import com.isil.edu.pe.spedidos.entity.AnulacionPedido;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.entity.PedidoEstado;
import com.isil.edu.pe.spedidos.exception.AnulacionDuplicadaException;
import com.isil.edu.pe.spedidos.repository.AnulacionPedidoRepository;
import com.isil.edu.pe.spedidos.repository.PedidoRepository;import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnulacionPedidoServiceImplTest {

    private PedidoRepository pedidoRepository;
    private AnulacionPedidoRepository anulacionPedidoRepository;
    private DevolucionStockClient devolucionStockClient;
    private AnulacionPedidoServiceImpl service;

    @BeforeEach
    void setUp() {
        pedidoRepository = mock(PedidoRepository.class);
        anulacionPedidoRepository = mock(AnulacionPedidoRepository.class);
        devolucionStockClient = mock(DevolucionStockClient.class);
        service = new AnulacionPedidoServiceImpl(
                pedidoRepository,
                anulacionPedidoRepository,
                devolucionStockClient
        );
    }

    @Test
    void anulacionCorrectaInvocaUnaDevolucionMarcaAnuladoYGuardaRegistro() {
        // preparación
        Pedido pedido = pedidoRegistrado(1L, "LAP-001", 2);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(anulacionPedidoRepository.existsByPedidoId(1L)).thenReturn(false);
        when(devolucionStockClient.devolverStock("LAP-001", 2))
                .thenReturn(new DevolucionStockResponse());
        when(anulacionPedidoRepository.save(any(AnulacionPedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ejecución
        AnulacionPedido resultado = service.anular(1L, "Solicitud del cliente");

        // verificación
        assertEquals(PedidoEstado.ANULADO, pedido.getEstado());
        assertEquals(1L, resultado.getPedidoId());
        assertEquals("LAP-001", resultado.getProductoCodigo());
        assertEquals(2, resultado.getCantidadDevuelta());
        verify(devolucionStockClient, times(1)).devolverStock("LAP-001", 2);
        verify(pedidoRepository).save(pedido);
        verify(anulacionPedidoRepository).save(any(AnulacionPedido.class));
    }

    @Test
    void anulacionDuplicadaSeRechazaSinNuevaDevolucionNiRegistroAdicional() {
        Pedido pedido = pedidoRegistrado(1L, "LAP-001", 2);
        pedido.setEstado(PedidoEstado.ANULADO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThrows(AnulacionDuplicadaException.class, () -> service.anular(1L, "otra vez"));

        verifyNoInteractions(devolucionStockClient);
        verify(anulacionPedidoRepository, never()).save(any(AnulacionPedido.class));
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void falloDeInventarioNoConfirmaCambiosLocalesNiRegistraAnulacion() {
        Pedido pedido = pedidoRegistrado(1L, "LAP-001", 2);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(anulacionPedidoRepository.existsByPedidoId(1L)).thenReturn(false);
        when(devolucionStockClient.devolverStock("LAP-001", 2))
                .thenThrow(new InventarioNoDisponibleException("Inventario caído"));

        assertThrows(InventarioNoDisponibleException.class, () -> service.anular(1L, "Solicitud"));

        assertEquals(PedidoEstado.REGISTRADO, pedido.getEstado());
        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(anulacionPedidoRepository, never()).save(any(AnulacionPedido.class));
    }

    private Pedido pedidoRegistrado(Long id, String codigo, int cantidad) {
        return new Pedido(
                id,
                "Ana Torres",
                codigo,
                "Laptop",
                new BigDecimal("2500.00"),
                cantidad,
                new BigDecimal("2500.00").multiply(BigDecimal.valueOf(cantidad)),
                LocalDateTime.now()
        );
    }
}
