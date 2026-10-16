package com.isil.edu.pe.spedidos.service.impl;

import com.isil.edu.pe.spedidos.client.InventarioClient;
import com.isil.edu.pe.spedidos.client.InventarioClientException;
import com.isil.edu.pe.spedidos.client.InventarioNoDisponibleException;
import com.isil.edu.pe.spedidos.client.dto.ProductoInventarioResponse;
import com.isil.edu.pe.spedidos.client.dto.ReservaStockResponse;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.exception.PedidoException;
import com.isil.edu.pe.spedidos.repository.PedidoRepository;
import com.isil.edu.pe.spedidos.service.PedidoService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isil.edu.pe.spedidos.client.DevolucionStockClient;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
/**
 * Implementación de las operaciones de negocio definidas por
 * {@link PedidoService}.
 *
 * <p>Esta clase contiene la lógica de negocio del Sistema de Pedidos y
 * coordina dos dependencias principales:</p>
 *
 * <ul>
 *   <li>
 *     {@link PedidoRepository}, encargado de la persistencia local
 *     de los pedidos.
 *   </li>
 *   <li>
 *     {@link InventarioClient}, encargado de comunicarse con
 *     {@code inventario-service}.
 *   </li>
 * </ul>
 *
 * <p>La anotación {@link Service @Service} indica a Spring que esta
 * clase forma parte de la capa de servicios y que debe ser registrada
 * como un bean dentro del contenedor IoC.</p>
 *
 * <p>Las dependencias se reciben mediante Constructor Injection:</p>
 *
 * <pre>
 * Spring IoC Container
 *        |
 *        +--> PedidoRepository
 *        |
 *        +--> InventarioClient
 *                 |
 *                 v
 *         PedidoServiceImpl
 * </pre>
 *
 * <p>La clase no construye directamente sus dependencias mediante
 * {@code new}. Spring se encarga de localizarlas, crearlas e
 * inyectarlas al construir este bean.</p>
 *
 * @see PedidoService
 * @see PedidoRepository
 * @see InventarioClient
 */
@Service
public class PedidoServiceImpl implements PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoServiceImpl.class);

    private final PedidoRepository pedidoRepository;
    private final InventarioClient inventarioClient;
    private final DevolucionStockClient devolucionStockClient;
    private final boolean simularFalloPostReserva;

  /**
   * Construye el servicio utilizando las dependencias proporcionadas
   * por el contenedor IoC de Spring.
   *
   * <p>Conceptualmente, Spring realiza una operación semejante a:</p>
   *
   * <pre>
   * new PedidoServiceImpl(
   *     pedidoRepository,
   *     inventarioClient
   * );
   * </pre>
   *
   * <p>Como existe un único constructor, no es necesario utilizar
   * {@code @Autowired}.</p>
   *
   * @param pedidoRepository repositorio utilizado para almacenar
   *                         y consultar pedidos
   * @param inventarioClient cliente utilizado para comunicarse
   *                         con el servicio de Inventario
   */
  public PedidoServiceImpl(
          PedidoRepository pedidoRepository,
          InventarioClient inventarioClient,
          DevolucionStockClient devolucionStockClient,
          @Value("${pedidos.simular-fallo-post-reserva:false}") boolean simularFalloPostReserva) {
      this.pedidoRepository = pedidoRepository;
      this.inventarioClient = inventarioClient;
      this.devolucionStockClient = devolucionStockClient;
      this.simularFalloPostReserva = simularFalloPostReserva;
  }

  /**
   * Registra un pedido coordinando la lógica local con el servicio
   * externo de Inventario.
   *
   * <p>La operación realiza las siguientes acciones:</p>
   *
   * <ol>
   *   <li>Valida los datos recibidos.</li>
   *   <li>Solicita la reserva de stock a Inventario.</li>
   *   <li>Valida la información retornada.</li>
   *   <li>Calcula el importe total.</li>
   *   <li>Construye la entidad {@link Pedido}.</li>
   *   <li>Persiste el pedido mediante {@link PedidoRepository}.</li>
   * </ol>
   *
   * <p>{@link Transactional @Transactional} controla la transacción
   * correspondiente a la persistencia local del pedido.</p>
   *
   * <p>Debe tenerse en cuenta que esta transacción no incluye la
   * operación remota realizada sobre {@code inventario-service},
   * porque dicha aplicación posee su propia transacción y base de
   * datos.</p>
   *
   * @param cliente nombre del cliente
   * @param productoCodigo código del producto
   * @param cantidad cantidad solicitada
   * @return pedido registrado
   * @throws PedidoException si la solicitud contiene datos inválidos
   *         o se produce un error de negocio durante la reserva
   */
  @Override
  @Transactional
  public Pedido registrarPedido(String cliente, String productoCodigo, int cantidad) {
      validarDatos(cliente, productoCodigo, cantidad);

      final ReservaStockResponse reserva;

      try {
          reserva = inventarioClient.reservarStock(productoCodigo.trim(), cantidad);
      } catch (InventarioClientException e) {
          if (e.getStatus() >= 500) {
              throw new InventarioNoDisponibleException(
                      "El servicio de inventario presentó un error temporal (HTTP "
                              + e.getStatus()
                              + ").",
                      e
              );
          }
          throw new PedidoException(e.getMessage(), e);
      }

      validarReserva(reserva);

      BigDecimal total = reserva.getPrecio().multiply(BigDecimal.valueOf(cantidad));

      Pedido pedido = new Pedido(cliente.trim(), reserva.getCodigo(), reserva.getNombre(),
              reserva.getPrecio(), cantidad, total);

      try {
          if (simularFalloPostReserva) {
              throw new PedidoException("Fallo local simulado después de la reserva (solo pruebas).");
          }
          return pedidoRepository.save(pedido);
      } catch (RuntimeException e) {
          // La reserva remota ya fue confirmada: hay que deshacerla con la API de devolución.
          compensarReserva(productoCodigo.trim(), cantidad);
          throw e; // al relanzar, @Transactional revierte lo local en H2
      }
  }

    @Override
    public List<ProductoInventarioResponse> listarProductos() {
        return inventarioClient.listarProductos();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAllByOrderByIdDesc();
    }

    private void compensarReserva(String codigo, int cantidad) {
        try {
            devolucionStockClient.devolverStock(codigo, cantidad);
            log.warn("Reserva compensada: se devolvieron {} unidades de {}.", cantidad, codigo);
        } catch (RuntimeException ex) {
            log.error("INCIDENTE PENDIENTE DE CONCILIACION: no se pudo devolver {} unidades de {}.", cantidad, codigo, ex);
        }
    }

    private void validarDatos(String cliente, String productoCodigo, int cantidad) {
        if (cliente == null || cliente.isBlank()) {
            throw new PedidoException(
                    "El cliente es obligatorio."
            );
        }

        if (productoCodigo == null
                || productoCodigo.isBlank()) {
            throw new PedidoException(
                    "Debe seleccionar un producto."
            );
        }

        if (cantidad <= 0) {
            throw new PedidoException(
                    "La cantidad debe ser mayor que cero."
            );
        }
    }

    private void validarReserva(ReservaStockResponse reserva) {
        if (reserva == null
                || reserva.getCodigo() == null
                || reserva.getCodigo().isBlank()
                || reserva.getNombre() == null
                || reserva.getNombre().isBlank()
                || reserva.getPrecio() == null) {
            throw new PedidoException(
                    "inventario-service devolvió "
                            + "una respuesta incompleta."
            );
        }
    }
}
