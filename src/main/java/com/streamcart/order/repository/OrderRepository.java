package com.streamcart.order.repository;

import com.streamcart.order.entity.Order;
import com.streamcart.order.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUser(User user);
    List<Order> findByUser_Username(String username);
}
