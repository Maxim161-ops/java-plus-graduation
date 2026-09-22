package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dao.UserRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.interaction.dto.user.UserResponse;

import java.util.List;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .build())
                .orElseThrow(() ->
                        new NotFoundException(
                                "User id: " + userId + " not found."
                        ));
    }

    @GetMapping
    public List<UserResponse> getUsers(
            @RequestParam("ids") List<Long> ids) {

        return userRepository.findAllById(ids).stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .build())
                .toList();
    }
}
