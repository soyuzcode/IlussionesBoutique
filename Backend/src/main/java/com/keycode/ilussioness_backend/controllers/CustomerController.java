package com.keycode.ilussioness_backend.controllers;

import com.keycode.ilussioness_backend.models.Customer;
import com.keycode.ilussioness_backend.repositories.CustomerRepository;
import com.keycode.ilussioness_backend.services.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@CrossOrigin(origins = "*") // Permite llamadas desde el frontend
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final CustomerService customerService;

    public CustomerController(
            CustomerRepository customerRepository,
            CustomerService customerService
    ) {
        this.customerRepository = customerRepository;
        this.customerService = customerService;
    }

    @GetMapping
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @PostMapping
    public Customer createCustomer(@RequestBody Customer customer) {
        return customerService.createCustomer(customer);
    }

    // --- NUEVO MÉTODO PARA REMOVER CLIENTE ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
        if (!customerService.deleteCustomer(id)) {
            return ResponseEntity.notFound().build(); // Si no existe el id, devuelve 404
        }
        return ResponseEntity.noContent().build(); // Devuelve 204 si se eliminó con éxito
    }

    // --- NUEVO MÉTODO PARA ACTUALIZAR CLIENTE ---
    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable UUID id, @RequestBody Customer updatedCustomer) {
        return customerService.updateCustomer(id, updatedCustomer)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- NUEVO MÉTODO PARA BUSCAR POR ID ---
    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable UUID id) {
        return customerRepository.findById(id)
            .map(customer -> ResponseEntity.ok(customer))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}