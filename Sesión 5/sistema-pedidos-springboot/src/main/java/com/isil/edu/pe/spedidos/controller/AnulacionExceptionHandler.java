package com.isil.edu.pe.spedidos.controller;
import com.isil.edu.pe.spedidos.client.InventarioNoDisponibleException;
import com.isil.edu.pe.spedidos.controller.dto.ApiErrorResponse;
import com.isil.edu.pe.spedidos.exception.AnulacionDuplicadaException;
import com.isil.edu.pe.spedidos.exception.PedidoException;
import com.isil.edu.pe.spedidos.exception.PedidoNoEncontradoException;
import java.time.LocalDateTime;import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;



@RestControllerAdvice(assignableTypes = AnulacionPedidoController.class)
public class AnulacionExceptionHandler {

    @ExceptionHandler(PedidoNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> pedidoNoEncontrado(PedidoNoEncontradoException e) {
        return construir(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(AnulacionDuplicadaException.class)
    public ResponseEntity<ApiErrorResponse> anulacionDuplicada(AnulacionDuplicadaException e) {
        return construir(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(InventarioNoDisponibleException.class)
    public ResponseEntity<ApiErrorResponse> inventarioNoDisponible(InventarioNoDisponibleException e) {
        return construir(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(PedidoException.class)
    public ResponseEntity<ApiErrorResponse> errorDeNegocio(PedidoException e) {
        return construir(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> construir(HttpStatus status, String mensaje) {
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(body);
    }
}
