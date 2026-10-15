package com.isil.edu.pe.spedidos.service.impl;

import com.isil.edu.pe.spedidos.client.DevolucionStockClient;
import com.isil.edu.pe.spedidos.client.InventarioClientException;
import com.isil.edu.pe.spedidos.client.InventarioNoDisponibleException;
import com.isil.edu.pe.spedidos.entity.AnulacionPedido;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.entity.PedidoEstado;
import com.isil.edu.pe.spedidos.exception.AnulacionDuplicadaException;
import com.isil.edu.pe.spedidos.exception.PedidoException;
import com.isil.edu.pe.spedidos.exception.PedidoNoEncontradoException;
import com.isil.edu.pe.spedidos.repository.AnulacionPedidoRepository;
import com.isil.edu.pe.spedidos.repository.PedidoRepository;
import com.isil.edu.pe.spedidos.service.AnulacionPedidoService;
import java.util.List;import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AnulacionPedidoServiceImpl implements AnulacionPedidoService {

    private final PedidoRepository pedidoRepository;
    private final AnulacionPedidoRepository anulacionPedidoRepository;
    private final DevolucionStockClient devolucionStockClient;

    public AnulacionPedidoServiceImpl( PedidoRepository pedidoRepository,
                                       AnulacionPedidoRepository anulacionPedidoRepository,
                                       DevolucionStockClient devolucionStockClient) {
        this.pedidoRepository = pedidoRepository;
        this.anulacionPedidoRepository = anulacionPedidoRepository;
        this.devolucionStockClient = devolucionStockClient; }

    @Override

    @Transactional
    public AnulacionPedido anular(Long pedidoId, String motivo) {
        if (pedidoId == null) {
            throw new PedidoException("El pedidoId es obligatorio.");
        }

        // 1. El pedido debe existir.
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new PedidoNoEncontradoException(pedidoId));

        // 2. Debe estar REGISTRADO y no tener anulación previa.
        if (pedido.getEstado() == PedidoEstado.ANULADO || anulacionPedidoRepository.existsByPedidoId(pedidoId)) {
            throw new AnulacionDuplicadaException(pedidoId);
        }

        // 3. Devolución remota. Si falla, salimos ANTES de tocar H2.
        try {
            devolucionStockClient.devolverStock(pedido.getProductoCodigo(), pedido.getCantidad());
        } catch (InventarioClientException e) {
            if (e.getStatus() >= 500) {
                throw new InventarioNoDisponibleException(
                        "El servicio de inventario presentó un error temporal (HTTP " + e.getStatus() + ").",
                        e
                );
            }
            throw new PedidoException(e.getMessage(), e);
        }

        // 4. Inventario confirmó: registramos la anulación y cambiamos el estado (no se borra el pedido).
        AnulacionPedido anulacion = new AnulacionPedido(
                pedido.getId(),
                pedido.getProductoCodigo(),
                pedido.getCantidad(),
                limpiarMotivo(motivo)
        );

        pedido.setEstado(PedidoEstado.ANULADO);
        pedidoRepository.save(pedido);

        return anulacionPedidoRepository.save(anulacion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnulacionPedido> listar() {
        return anulacionPedidoRepository.findAllByOrderByIdDesc();
    }

    private String limpiarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return null;
        }
        String limpio = motivo.trim();
        return limpio.length() > 255 ? limpio.substring(0, 255) : limpio;
    }
}
