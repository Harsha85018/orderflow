package io.github.harsha85018.orderflow.notification;

import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepository notifications;

    public NotificationController(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<Notification> list(@RequestParam(required = false) String customerId) {
        return customerId == null
                ? notifications.findAll()
                : notifications.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }
}
