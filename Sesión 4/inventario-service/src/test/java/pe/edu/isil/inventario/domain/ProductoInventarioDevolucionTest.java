package pe.edu.isil.inventario.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductoInventarioDevolucionTest {

    private ProductoInventario productoConStock(int stock) {
        return new ProductoInventario("LAP-001", "Laptop", new BigDecimal("2500.00"), stock);
    }

    @Test
    void devolucionValidaIncrementaElStockExactamente() {
        // preparación
        ProductoInventario producto = productoConStock(8);

        // ejecución
        producto.devolverStock(2);

        // verificación: 8 + 2 = 10
        assertEquals(10, producto.getStock());
    }

    @Test
    void devolucionInvalidaSeRechazaYNoCambiaElStock() {
        ProductoInventario producto = productoConStock(8);

        assertThrows(IllegalArgumentException.class, () -> producto.devolverStock(0));
        assertThrows(IllegalArgumentException.class, () -> producto.devolverStock(-3));

        assertEquals(8, producto.getStock());
    }
}