package com.keycode.ilussioness_backend.models;

public abstract class User {
    protected String userId;
    protected String name;
    protected String password;
    protected String role;

    public User() {}

    public User(String userId, String name, String password, String role) {
        this.userId = userId;
        this.name = name;
        this.password = password;
        this.role = role;
    }

    public boolean logIn() {
        return true;
    }

    public void logOut() {
        // cerrar sesión
    }

    public void changePassword() {
        // cambiar contraseña
    }

    // Getters y Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}