package io.github.harsha85018.orderflow.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository payments;

    public PaymentController(PaymentService paymentService, PaymentRepository payments) {
        this.paymentService = paymentService;
        this.payments = payments;
    }

    public record ChargeRequest(@NotNull UUID orderId, @Min(1) long amountCents) {}

    @PostMapping
    public ResponseEntity<Payment> charge(@Valid @RequestBody ChargeRequest req) {
        var result = paymentService.charge(req.orderId(), req.amountCents());
        HttpStatus code = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(code).body(result.payment());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> get(@PathVariable UUID id) {
        return payments.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
