package raicod3.example.com.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import raicod3.example.com.annotation.Auditable;
import raicod3.example.com.constants.Http_Constants;
import raicod3.example.com.dto.customer.UserAddressDto;
import raicod3.example.com.dto.user.UserResponseDto;
import raicod3.example.com.exception.UnauthorizedException;
import raicod3.example.com.model.User;
import raicod3.example.com.model.UserAddress;
import raicod3.example.com.repository.UserAddressRepository;
import raicod3.example.com.repository.UserRepository;
import raicod3.example.com.utilities.APIResponse;

import java.util.UUID;

@Slf4j
@Service
public class UserAddressService {

    private final UserAddressRepository userAddressRepo;
    private final UserService userService;
    private final UserRepository userRepository;

    public UserAddressService(UserAddressRepository userAddressRepo, UserService userService, UserRepository userRepository) {
        this.userAddressRepo = userAddressRepo;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Auditable(action = "USER_ADDRESS_UPDATE")
    public APIResponse addUserAddress(UUID userId, UserAddressDto addressDto) {

        log.debug("Validating user...");
        User user = userRepository.findById(userId).orElseThrow(() -> new UnauthorizedException("Unauthorized to perfomr this action!"));

        log.debug("Adding user address...");
        UserAddress newAddress = new UserAddress();
        newAddress.setUser(user);
        newAddress.setLatitude(addressDto.getLatitude());
        newAddress.setLongitude(addressDto.getLongitude());
        newAddress.setFormattedAddress(addressDto.getFormattedAddress());

        userAddressRepo.save(newAddress);
        log.info("User successfully added address...");

        return APIResponse.success(newAddress,"Added user address successfully", Http_Constants.CREATED);
    }
}
