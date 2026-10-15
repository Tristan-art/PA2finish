package com.isil.edu.pe.spedidos.controller;

import com.isil.edu.pe.spedidos.controller.dto.AnulacionRequest;
import com.isil.edu.pe.spedidos.controller.dto.AnulacionResponse;
import com.isil.edu.pe.spedidos.service.AnulacionPedidoService;
import java.util.List;import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/anulaciones")
public class AnulacionPedidoController {

    private final AnulacionPedidoService anulacionPedidoService;
    public AnulacionPedidoController(AnulacionPedidoService anulacionPedidoService)
    { this.anulacionPedidoService = anulacionPedidoService; }
    @PostMapping
    public ResponseEntity<AnulacionResponse> anular(@RequestBody AnulacionRequest request) {
        AnulacionResponse respuesta = AnulacionResponse.from(
                anulacionPedidoService.anular(request.pedidoId(), request.motivo())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public List<AnulacionResponse> listar() {
        return anulacionPedidoService.listar()
                .stream()
                .map(AnulacionResponse::from)
                .toList();
    }

}
