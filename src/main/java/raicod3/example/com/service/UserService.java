package raicod3.example.com.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import raicod3.example.com.annotation.Auditable;
import raicod3.example.com.custom.CustomUserDetails;
import raicod3.example.com.dto.user.UserResponseDto;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.model.User;
import raicod3.example.com.repository.UserRepository;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;


    public UserService (UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);

        return users.map(UserResponseDto::new);
    }

    public UserResponseDto loggedInUser(CustomUserDetails customUserDetails) {
        User user = userRepository.findUserByEmail(customUserDetails.getUsername()).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new UserResponseDto(user);
    }

    public UserResponseDto getUserById(UUID id) {

        User existingUser = userRepository.findById(id).orElseThrow(() ->  new ResourceNotFoundException("User not found."));

        return new UserResponseDto(existingUser);
    }

    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found."));

        return new UserResponseDto(user);
    }


}
