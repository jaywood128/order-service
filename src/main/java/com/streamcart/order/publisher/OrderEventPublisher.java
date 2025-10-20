package com.streamcart.order.publisher;

import com.streamcart.order.dto.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {
    
    private static final String TOPIC = "order.created";
    
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    
    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing order created event for order: {}", event.orderId());
        
        CompletableFuture<SendResult<String, OrderCreatedEvent>> future = 
            kafkaTemplate.send(TOPIC, event.orderId(), event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully published order created event for order: {} to partition: {}", 
                    event.orderId(), 
                    result.getRecordMetadata().partition());
            } else {
                log.error("Failed to publish order created event for order: {}", 
                    event.orderId(), ex);
            }
        });
    }
}
