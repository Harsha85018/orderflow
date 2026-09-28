package io.github.harsha85018.orderflow.inventory;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryService {

    public record ReserveResult(Reservation reservation, boolean created) {}

    private final ProductRepository products;
    private final ReservationRepository reservations;

    public InventoryService(ProductRepository products, ReservationRepository reservations) {
        this.products = products;
        this.reservations = reservations;
    }

    @Transactional
    public ReserveResult reserve(UUID orderId, String productId, int quantity) {
        // Idempotency: one reservation per order, ever.
        var existing = reservations.findByOrderId(orderId);
        if (existing.isPresent()) {
            return new ReserveResult(existing.get(), false);
        }

        // Locks the product row until this transaction commits, so two
        // requests can never both read "1 left" and both take it.
        Product product = products.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown product"));

        ReservationStatus status = product.tryReserve(quantity)
                ? ReservationStatus.RESERVED
                : ReservationStatus.REJECTED;

        Reservation reservation = reservations.save(
                new Reservation(orderId, productId, quantity, status));
        return new ReserveResult(reservation, true);
    }

    /** Compensation step: gives stock back. Safe to call more than once. */
    @Transactional
    public Reservation release(UUID orderId) {
        Reservation reservation = reservations.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "no reservation"));

        if (reservation.getStatus() == ReservationStatus.RESERVED) {
            Product product = products.findByIdForUpdate(reservation.getProductId()).orElseThrow();
            product.release(reservation.getQuantity());
            reservation.markReleased();
        }
        return reservation;
    }
}
