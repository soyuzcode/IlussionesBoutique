package com.keycode.ilussioness_backend.services;

import com.keycode.ilussioness_backend.audit.domain.AuditEntityType;
import com.keycode.ilussioness_backend.audit.domain.AuditEventType;
import com.keycode.ilussioness_backend.audit.domain.AuditOutcome;
import com.keycode.ilussioness_backend.audit.service.AuditService;
import com.keycode.ilussioness_backend.models.Appointment;
import com.keycode.ilussioness_backend.models.AppointmentStatus;
import com.keycode.ilussioness_backend.repositories.AppointmentRepository;
import com.keycode.ilussioness_backend.repositories.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AppointmentService {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            CustomerRepository customerRepository,
            AuditService auditService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Appointment> getAppointmentById(UUID id) {
        return appointmentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsByCustomer(UUID customerId) {
        return appointmentRepository.findByCustomerId(customerId);
    }

    @Transactional
    public Appointment createAppointment(Appointment appointment) {
        try {
            requireValidAppointment(appointment);
        } catch (BusinessValidationException ex) {
            auditFailure(
                    AuditEventType.APPOINTMENT_CREATE_FAILED,
                    appointment == null || appointment.getId() == null
                            ? null : appointment.getId().toString(),
                    AuditOutcome.REJECTED,
                    BusinessValidationException.auditReason(ex.getCode())
            );
            throw ex;
        }
        if (appointment.getStatus() == null) {
            appointment.setStatus(AppointmentStatus.SCHEDULED);
        }

        Appointment saved;
        try {
            saved = appointmentRepository.saveAndFlush(appointment);
        } catch (RuntimeException ex) {
            auditFailure(
                    AuditEventType.APPOINTMENT_CREATE_FAILED,
                    null,
                    AuditOutcome.FAILURE,
                    "PERSISTENCE_ERROR"
            );
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

    /**
     * Applies the non-null fields of {@code updates} to the stored appointment.
     *
     * @return the updated appointment, or empty when it did not exist.
     */
    @Transactional
    public Optional<Appointment> updateAppointment(UUID id, Appointment updates) {
        if (id == null) {
            throw new BusinessValidationException("id_required");
        }
        Optional<Appointment> existing = appointmentRepository.findById(id);
        if (existing.isEmpty()) {
            auditFailure(
                    AuditEventType.APPOINTMENT_UPDATE_REJECTED,
                    id.toString(),
                    AuditOutcome.REJECTED,
                    "NOT_FOUND"
            );
            return Optional.empty();
        }

        Appointment appointment = existing.get();
        try {
            if (updates != null) {
                if (updates.getReason() != null) {
                    appointment.setReason(updates.getReason());
                }
                if (updates.getAppointmentDate() != null) {
                    appointment.setAppointmentDate(updates.getAppointmentDate());
                }
                if (updates.getAppointmentTime() != null) {
                    appointment.setAppointmentTime(updates.getAppointmentTime());
                }
                if (updates.getStatus() != null) {
                    appointment.setStatus(updates.getStatus());
                }
                if (updates.getGoogleEventId() != null) {
                    appointment.setGoogleEventId(updates.getGoogleEventId());
                }
                if (updates.getCustomer() != null && updates.getCustomer().getId() != null) {
                    UUID customerId = updates.getCustomer().getId();
                    if (!customerRepository.existsById(customerId)) {
                        throw new BusinessValidationException("customer_not_found");
                    }
                    appointment.setCustomer(updates.getCustomer());
                }
            }
            requireValidAppointment(appointment);
        } catch (BusinessValidationException ex) {
            auditFailure(
                    AuditEventType.APPOINTMENT_UPDATE_REJECTED,
                    id.toString(),
                    AuditOutcome.REJECTED,
                    BusinessValidationException.auditReason(ex.getCode())
            );
            throw ex;
        }

        Appointment saved;
        try {
            saved = appointmentRepository.saveAndFlush(appointment);
        } catch (RuntimeException ex) {
            auditFailure(
                    AuditEventType.APPOINTMENT_UPDATE_FAILED,
                    id.toString(),
                    AuditOutcome.FAILURE,
                    "PERSISTENCE_ERROR"
            );
            throw ex;
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        if (saved.getStatus() != null) {
            metadata.put("APPOINTMENT_STATUS", saved.getStatus().name());
        }
        auditService.recordBusinessSuccess(
                AuditEventType.APPOINTMENT_UPDATED,
                AuditEntityType.APPOINTMENT,
                id.toString(),
                metadata
        );
        return Optional.of(saved);
    }

    /**
     * @return true when deleted, false when the appointment did not exist.
     */
    @Transactional
    public boolean deleteAppointment(UUID id) {
        if (id == null) {
            throw new BusinessValidationException("id_required");
        }
        if (!appointmentRepository.existsById(id)) {
            auditFailure(
                    AuditEventType.APPOINTMENT_DELETE_REJECTED,
                    id.toString(),
                    AuditOutcome.REJECTED,
                    "NOT_FOUND"
            );
            return false;
        }

        try {
            appointmentRepository.deleteById(id);
            appointmentRepository.flush();
        } catch (RuntimeException ex) {
            auditFailure(
                    AuditEventType.APPOINTMENT_DELETE_FAILED,
                    id.toString(),
                    AuditOutcome.FAILURE,
                    "PERSISTENCE_ERROR"
            );
            throw ex;
        }

        auditService.recordBusinessSuccess(
                AuditEventType.APPOINTMENT_DELETED,
                AuditEntityType.APPOINTMENT,
                id.toString(),
                Map.of()
        );
        return true;
    }

    /**
     * Enforces the appointment invariants: a customer reference that exists and
     * the mandatory scheduling fields.
     */
    private void requireValidAppointment(Appointment appointment) {
        if (appointment == null) {
            throw new BusinessValidationException("payload_required");
        }
        if (appointment.getCustomer() == null || appointment.getCustomer().getId() == null) {
            throw new BusinessValidationException("customer_required");
        }
        if (!customerRepository.existsById(appointment.getCustomer().getId())) {
            throw new BusinessValidationException("customer_not_found");
        }
        if (appointment.getAppointmentDate() == null) {
            throw new BusinessValidationException("appointment_date_required");
        }
        if (appointment.getAppointmentTime() == null) {
            throw new BusinessValidationException("appointment_time_required");
        }
        if (appointment.getReason() != null && appointment.getReason().length() > 500) {
            throw new BusinessValidationException("reason_too_long");
        }
    }

    private void auditFailure(
            AuditEventType eventType,
            String entityId,
            AuditOutcome outcome,
            String reason
    ) {
        try {
            auditService.recordBusinessFailure(
                    eventType,
                    AuditEntityType.APPOINTMENT,
                    entityId,
                    outcome,
                    Map.of("REASON", reason)
            );
        } catch (RuntimeException auditException) {
            LOG.warn("Could not persist appointment audit failure {} ({})",
                    eventType, auditException.getClass().getSimpleName());
        }
    }
}
