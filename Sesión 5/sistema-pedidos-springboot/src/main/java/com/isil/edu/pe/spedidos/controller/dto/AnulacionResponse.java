package com.isil.edu.pe.spedidos.controller.dto;

import com.isil.edu.pe.spedidos.entity.AnulacionPedido;
import java.time.LocalDateTime;

public record AnulacionResponse( Long id, Long pedidoId,
                                 String productoCodigo,
                                 int cantidadDevuelta,
                                 LocalDateTime fecha,
                                 String motivo)
{
    public static AnulacionResponse from(AnulacionPedido anulacion) {
        return new AnulacionResponse(
                anulacion.getId(),
                anulacion.getPedidoId(),

                anulacion.getProductoCodigo(),
                anulacion.getCantidadDevuelta(),
                anulacion.getFecha(),
                anulacion.getMotivo() );
    }
}
