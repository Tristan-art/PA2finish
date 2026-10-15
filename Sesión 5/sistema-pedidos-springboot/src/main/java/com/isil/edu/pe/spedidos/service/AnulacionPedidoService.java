package com.isil.edu.pe.spedidos.service;

import com.isil.edu.pe.spedidos.entity.AnulacionPedido;
import java.util.List;


public interface AnulacionPedidoService {

    AnulacionPedido anular(Long pedidoId, String motivo);

    List<AnulacionPedido> listar();

}
