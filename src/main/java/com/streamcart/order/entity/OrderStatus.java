package com.streamcart.order.entity;

public enum OrderStatus {
    PENDING,
    PAYMENT_PROCESSING,
    PAID,
    INVENTORY_RESERVED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    FAILED
}
