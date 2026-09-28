package io.github.harsha85018.orderflow.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class InventoryController {

    private final InventoryService inventory;
    private final ProductRepository products;

    public InventoryController(InventoryService inventory, ProductRepository products) {
        this.inventory = inventory;
        this.products = products;
    }

    public record ReserveRequest(
            @NotNull UUID orderId,
            @NotBlank String productId,
            @Min(1) int quantity) {}

    @PostMapping("/reservations")
    public ResponseEntity<Reservation> reserve(@Valid @RequestBody ReserveRequest req) {
        var result = inventory.reserve(req.orderId(), req.productId(), req.quantity());
        HttpStatus code = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(code).body(result.reservation());
    }

    @PostMapping("/reservations/{orderId}/release")
    public Reservation release(@PathVariable UUID orderId) {
        return inventory.release(orderId);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> product(@PathVariable String id) {
        return products.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
