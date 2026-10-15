package com.isil.edu.pe.spedidos.exception;


public class AnulacionDuplicadaException extends PedidoException{
    public AnulacionDuplicadaException(Long pedidoId) {
        super("El pedido " + pedidoId + " ya fue anulado.");
    }

}
