package com.example.employee;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "*")
public class EmployeeController {

    @Autowired
    private EmployeeRepository repository;

    @GetMapping
    public List<Employee> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public Employee create(@RequestBody Employee employee) {
        return repository.save(employee);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
