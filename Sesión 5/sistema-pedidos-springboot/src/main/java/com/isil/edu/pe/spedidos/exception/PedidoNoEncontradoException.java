package com.isil.edu.pe.spedidos.exception;

public class PedidoNoEncontradoException extends PedidoException {

    public PedidoNoEncontradoException(Long pedidoId) {
        super("No existe un pedido con id " + pedidoId + ".");

    }

}
