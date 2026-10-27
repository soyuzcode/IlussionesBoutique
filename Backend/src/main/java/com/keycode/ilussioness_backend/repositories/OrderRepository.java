package com.keycode.ilussioness_backend.repositories;

import com.keycode.ilussioness_backend.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * {@code orders.order_id} is a TEXT identifier, so the repository id type must
 * stay {@link String} to match the entity and Supabase schema.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
}
