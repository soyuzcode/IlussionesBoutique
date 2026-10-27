package com.keycode.ilussioness_backend.repositories;

import com.keycode.ilussioness_backend.models.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    // Método para buscar el historial de citas por cliente
    List<Appointment> findByCustomerId(UUID customerId);
}