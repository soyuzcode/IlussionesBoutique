package com.keycode.ilussioness_backend.controllers;

import com.keycode.ilussioness_backend.models.Customer;
import com.keycode.ilussioness_backend.repositories.CustomerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@CrossOrigin(origins = "*") // Permite llamadas desde el frontend
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @PostMapping
    public Customer createCustomer(@RequestBody Customer customer) {
        return customerRepository.save(customer);
    }

    // --- NUEVO MÉTODO PARA REMOVER CLIENTE ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
        if (!customerRepository.existsById(id)) {
            return ResponseEntity.notFound().build(); // Si no existe el id, devuelve 404
        }
        customerRepository.deleteById(id);
        return ResponseEntity.noContent().build(); // Devuelve 204 si se eliminó con éxito
    }

    // --- NUEVO MÉTODO PARA ACTUALIZAR CLIENTE ---
    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable UUID id, @RequestBody Customer updatedCustomer) {
        return customerRepository.findById(id)
            .map(customer -> {
                customer.setName(updatedCustomer.getName());
                customer.setPhone(updatedCustomer.getPhone());
                Customer savedCustomer = customerRepository.save(customer);
                return ResponseEntity.ok(savedCustomer);
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}