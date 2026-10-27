package com.keycode.ilussioness_backend.services;

import com.keycode.ilussioness_backend.audit.domain.AuditEntityType;
import com.keycode.ilussioness_backend.audit.domain.AuditEventType;
import com.keycode.ilussioness_backend.audit.domain.AuditOutcome;
import com.keycode.ilussioness_backend.audit.service.AuditService;
import com.keycode.ilussioness_backend.models.Appointment;
import com.keycode.ilussioness_backend.repositories.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AppointmentService {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final AuditService auditService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AuditService auditService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Appointment createAppointment(Appointment appointment) {
        Appointment saved;
        try {
            saved = appointmentRepository.saveAndFlush(appointment);
        } catch (RuntimeException ex) {
            auditFailure(null, "PERSISTENCE_ERROR");
            throw ex;
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        if (saved.getCustomer() != null && saved.getCustomer().getId() != null) {
            metadata.put("CUSTOMER_ID", saved.getCustomer().getId().toString());
        }
        if (saved.getStatus() != null) {
            metadata.put("APPOINTMENT_STATUS", saved.getStatus().name());
        }

        auditService.recordBusinessSuccess(
                AuditEventType.APPOINTMENT_CREATED,
                AuditEntityType.APPOINTMENT,
                saved.getId() == null ? null : saved.getId().toString(),
                metadata
        );
        return saved;
    }

    private void auditFailure(String entityId, String reason) {
        try {
            auditService.recordBusinessFailure(
                    AuditEventType.APPOINTMENT_CREATE_FAILED,
                    AuditEntityType.APPOINTMENT,
                    entityId,
                    AuditOutcome.FAILURE,
                    Map.of("REASON", reason)
            );
        } catch (RuntimeException auditException) {
            LOG.warn("Could not persist appointment audit failure ({})",
                    auditException.getClass().getSimpleName());
        }
    }
}
