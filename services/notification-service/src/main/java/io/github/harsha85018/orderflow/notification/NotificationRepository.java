package io.github.harsha85018.orderflow.notification;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
