package com.stockflow.domain.services.impl;

import com.stockflow.api.requests.user.UserSignupRequestDTO;
import com.stockflow.api.requests.user.UserUpdateRequestDTO;
import com.stockflow.api.responses.user.UserResponseDTO;
import com.stockflow.domain.entities.User;
import com.stockflow.domain.repositories.UserRepository;
import com.stockflow.domain.services.interfaces.UserService;
import com.stockflow.exceptions.DataAlreadyInUseException;
import com.stockflow.exceptions.DataNotFoundException;
import com.stockflow.shared.enums.AuthRoles;
import com.stockflow.shared.mappers.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository repository, PasswordEncoder encoder, UserMapper userMapper) {
        this.repository = repository;
        this.encoder = encoder;
        this.userMapper = userMapper;
    }

    @Override
    public UserResponseDTO create(UserSignupRequestDTO userSignupRequestDTO) {
        logger.info("Attempting to create user with login: {}", userSignupRequestDTO.login());

        if (repository.existsByLogin(userSignupRequestDTO.login())) {
            logger.error("User creation failed: login {} already in use.", userSignupRequestDTO.login());
            throw new DataAlreadyInUseException("Login already in use");
        }

        User newUser = userMapper.toEntity(userSignupRequestDTO);
        newUser.setPassword(encoder.encode(newUser.getPassword()));
        newUser.setRole(AuthRoles.COMMON_USER);

        User savedUser = repository.save(newUser);
        logger.info("User created successfully with ID: {}", savedUser.getId());

        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponseDTO update(UserUpdateRequestDTO userUpdateRequestDTO) {
        logger.info("Attempting to update user with ID: {}", userUpdateRequestDTO.id());

        Optional<User> optionalUser = repository.findById(userUpdateRequestDTO.id());
        if (optionalUser.isPresent()) {
            User retrievedUser = optionalUser.get();

            if (!retrievedUser.getLogin().equals(userUpdateRequestDTO.login()) &&
                    repository.existsByLogin(userUpdateRequestDTO.login())) {
                logger.error("User update failed: login {} already in use.", userUpdateRequestDTO.login());
                throw new DataAlreadyInUseException("Login already in use");
            }

            retrievedUser.setName(userUpdateRequestDTO.name());
            retrievedUser.setLogin(userUpdateRequestDTO.login());
            retrievedUser.setPassword(encoder.encode(userUpdateRequestDTO.password()));

            User updatedUser = repository.save(retrievedUser);
            logger.info("User with ID: {} updated successfully.", updatedUser.getId());

            return userMapper.toResponse(updatedUser);
        } else {
            logger.error("User with ID: {} not found for update.", userUpdateRequestDTO.id());
            throw new DataNotFoundException("User not found");
        }
    }

    @Override
    public UserResponseDTO findById(UUID id) {
        logger.info("Searching for user with ID: {}", id);

        Optional<User> optionalUser = repository.findById(id);
        if (optionalUser.isPresent()) {
            logger.info("User found with ID: {}", id);
            return userMapper.toResponse(optionalUser.get());
        } else {
            logger.error("User with ID: {} not found.", id);
            throw new DataNotFoundException("User not found");
        }
    }

    @Override
    public List<UserResponseDTO> findAll() {
        logger.info("Retrieving all users.");
        List<UserResponseDTO> users = userMapper.toResponseList(repository.findAll());
        logger.info("Total users retrieved: {}", users.size());
        return users;
    }

    @Override
    public void delete(UUID id) {
        logger.info("Attempting to delete user with ID: {}", id);

        if (!repository.existsById(id)) {
            logger.error("User with ID: {} not found for deletion.", id);
            throw new DataNotFoundException("User not found");
        }

        repository.deleteById(id);
        logger.info("User with ID: {} deleted successfully.", id);
    }
}
