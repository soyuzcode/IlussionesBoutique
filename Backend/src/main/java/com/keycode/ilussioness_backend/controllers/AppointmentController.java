package com.keycode.ilussioness_backend.controllers;

import com.keycode.ilussioness_backend.models.Appointment;
import com.keycode.ilussioness_backend.repositories.AppointmentRepository;
import com.keycode.ilussioness_backend.services.AppointmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
