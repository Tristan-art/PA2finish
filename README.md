# sistema-pedidos-springboot — PA2

## Requisitos
Java 21, Maven (o mvnw), inventario-service desplegado en WildFly (8082) con SQL Server.

## Orden de arranque
SQL Server → WildFly (inventario-service) → Spring Boot (`./mvnw spring-boot:run`, puerto 8081).

## Configuración (sin secretos)
`inventario.api.base-url`, `inventario.api.connect-timeout-ms` y `inventario.api.read-timeout-ms` en `application.properties`. H2 es en memoria; no se guardan credenciales reales.

## Endpoints nuevos
- `POST /sistema-pedidos/api/v1/anulaciones` → 201 | 404 pedido inexistente | 409 ya anulado | 503 inventario no disponible
- `GET /sistema-pedidos/api/v1/anulaciones`
Ejemplo de solicitud:
{ "pedidoId": 1, "motivo": "Solicitud del cliente" }

## Pruebas
`./mvnw test` → `AnulacionPedidoServiceImplTest` (3 pruebas: anulación correcta, duplicada, fallo de inventario).

## Reproducción
1. Ejecutar 02_reset_stock_demo.sql. 
2. Registrar un pedido. 
3. Usar `requests/anulaciones.http`.

## Limitaciones
La devolución remota y la escritura en H2 no forman una transacción distribuida. No hay reintentos ni idempotencia entre sistemas.
