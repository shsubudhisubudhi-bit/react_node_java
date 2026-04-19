package com.developer.test.controller;

import com.developer.test.dto.CreateUserRequest;
import com.developer.test.dto.UsersResponse;
import com.developer.test.exception.ResourceNotFoundException;
import com.developer.test.model.User;
import com.developer.test.service.DataStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final DataStore dataStore;

    public UserController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @GetMapping
    public ResponseEntity<UsersResponse> getUsers() {
        List<User> users = dataStore.getUsers();
        return ResponseEntity.ok(new UsersResponse(users, users.size()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable int id) {
        User user = dataStore.getUserById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User " + id + " not found");
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = dataStore.addUser(request.getName(), request.getEmail(), request.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
}
