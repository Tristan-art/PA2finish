## PA2 – Gestión de Devoluciones de Inventario

### Requisitos
Java 21, Maven (o mvnw), WildFly para despliegue y SQL Server configurado.

### Orden de arranque
1. SQL Server → 2. WildFly (inventario-service) → 3. Validación de conexión.

### Configuración (sin secretos)
Revisar que `config/database.properties` no contenga credenciales reales (usar variables de entorno o placeholders seguros).

### Endpoints nuevos
- `POST /inventario-service/api/inventario/productos/codigo/{codigo}/devoluciones` → Restablece o procesa la devolución de stock según el código del producto.
  - **Ejemplo de solicitud (JSON):**
    ```json
    {
      "cantidad": 2,
      "motivo": "Devolución por anulación de pedido PA2"
    }
    ```

### Pruebas
- Ejecutar `ProductoInventarioDevolucionTest` (contiene 2 pruebas unitarias/integración para validar el correcto funcionamiento de las devoluciones).

### Reproducción
1. Asegurar que el servicio de inventario esté corriendo en WildFly.
2. Enviar la petición POST al endpoint de devoluciones con el código del producto.
3. Verificar la actualización del stock en la base de datos de SQL Server.

### Limitaciones
La comunicación depende de la disponibilidad del servidor de aplicaciones WildFly y no maneja transacciones distribuidas complejas automáticas de dos fases entre sistemas desacoplados.
