package com.keycode.ilussioness_backend.controllers;

import com.keycode.ilussioness_backend.models.Appointment;
import com.keycode.ilussioness_backend.repositories.AppointmentRepository;
import com.keycode.ilussioness_backend.services.AppointmentService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointment")
@CrossOrigin(origins = "*")
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;

    public AppointmentController(
            AppointmentRepository appointmentRepository,
            AppointmentService appointmentService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    @PostMapping
    public Appointment createAppointment(@RequestBody Appointment appointment) {
        return appointmentService.createAppointment(appointment);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Appointment>> getAppointmentsByCustomer(@PathVariable UUID customerId) {
        List<Appointment> appointments = appointmentService.getAppointmentsByCustomer(customerId);
        return ResponseEntity.ok(appointments);
    }
}
