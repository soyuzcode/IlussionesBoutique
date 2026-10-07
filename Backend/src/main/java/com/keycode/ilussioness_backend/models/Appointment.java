package com.keycode.ilussioness_backend.models;

import java.util.Date;

public class Appointment {
    private String appointmentId;
    private Date date;
    private String time; // 'hora: Time' del UML

    public Appointment() {}

    public Appointment(String appointmentId, Date date, String time) {
        this.appointmentId = appointmentId;
        this.date = date;
        this.time = time;
    }

    public void scheduleAppointment() {
        // agendar cita
    }

    public void cancelAppointment() {
        // cancelar cita
    }

    public void syncGoogleCalendar() {
        // sincronizar calendario de Google
    }

    // Getters y Setters
    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
}