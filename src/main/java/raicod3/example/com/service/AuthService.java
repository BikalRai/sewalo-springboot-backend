package raicod3.example.com.service;


import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;
import raicod3.example.com.annotation.Auditable;
import raicod3.example.com.constants.Http_Constants;
import raicod3.example.com.custom.CustomUserDetailsService;
import raicod3.example.com.dto.email.EmailRequest;
import raicod3.example.com.dto.google.GoogleLoginRequestDto;
import raicod3.example.com.dto.google.GoogleOnboardingRequestDto;
import raicod3.example.com.dto.user.AuthRegistrationRequestDto;
import raicod3.example.com.dto.user.AuthRequestDto;
import raicod3.example.com.dto.user.PasswordUpdateRequestDto;
import raicod3.example.com.dto.user.UserResponseDto;
import raicod3.example.com.enums.AuthProvider;
import raicod3.example.com.enums.TokenType;
import raicod3.example.com.enums.UserRole;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ForbiddenException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.exception.UnauthorizedException;
import raicod3.example.com.jwt.JwtUtils;
import raicod3.example.com.lib.rabbitmq.RabbitMQProducer;
import raicod3.example.com.model.*;
import raicod3.example.com.repository.OTPTokenRepository;
import raicod3.example.com.repository.ProviderCreditsRepository;
import raicod3.example.com.repository.RefreshTokenRepository;
import raicod3.example.com.repository.UserRepository;
import raicod3.example.com.utilities.APIResponse;
import raicod3.example.com.utilities.NumberHelper;
import raicod3.example.com.utilities.PasswordValidation;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OTPTokenService otpTokenService;
    private final OTPTokenRepository otpTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final RabbitMQProducer rabbitMQProducer;
    private final ProviderCreditsRepository providerCreditsRepository;


    public AuthService(UserRepository userRepository, AuthenticationManager authenticationManager, CustomUserDetailsService customUserDetailsService, JwtUtils jwtUtils, PasswordEncoder passwordEncoder, RefreshTokenRepository refreshTokenRepository, RabbitMQProducer rabbitMQProducer, OTPTokenService otpTokenService, OTPTokenRepository otpTokenRepository, RefreshTokenService refreshTokenService, ProviderCreditsRepository providerCreditsRepository) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.rabbitMQProducer = rabbitMQProducer;
        this.otpTokenService = otpTokenService;
        this.otpTokenRepository = otpTokenRepository;
        this.refreshTokenService = refreshTokenService;
        this.providerCreditsRepository = providerCreditsRepository;
    }

    @Transactional
    @Auditable(action = "REGISTER")
    public APIResponse registerUser(AuthRegistrationRequestDto request, HttpServletResponse response) {

        log.info("Registering account...");
        Optional<User> foundUser = userRepository.findUserByEmail(request.getEmail());

        if (foundUser.isPresent()) {
            log.info("Email already registered: {}", request.getEmail());
            throw new BadRequestException("Email already registered.");
        }

        log.info("Creating user...");
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        if (request.getRole() == null) {
            throw new BadRequestException("Role not supported.");
        }

        if (request.getRole().equalsIgnoreCase("provider")) {
            user.setRole(UserRole.PROVIDER);

            ProviderProfile providerProfile = new ProviderProfile();
            providerProfile.setUser(user);
            user.setProviderProfile(providerProfile);
        } else {
            user.setRole(UserRole.CUSTOMER);

            CustomerProfile customerProfile = new CustomerProfile();
            customerProfile.setUser(user);
            user.setCustomerProfile(customerProfile);
        }

        user.setCreatedAt(LocalDateTime.now());

        String passwordValidation = PasswordValidation.validatePassword(request.getPassword());

        if (!"Strong".equalsIgnoreCase(passwordValidation)) {
            throw new BadRequestException(passwordValidation);
        }

        log.debug("Generating secure access and refresh token...");
        String accessToken = jwtUtils.generateToken(user.getEmail());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());

        ResponseCookie responseCookie = createCookie(refreshToken);
        setCookieHeader(responseCookie.toString(), response);
        log.info("Created tokens..");

        User savedUser = userRepository.save(user);
        log.info("Created user");

        if(savedUser.getRole() == UserRole.PROVIDER){

            ProviderCredits providerCredits = new ProviderCredits();
            providerCredits.setProvider(savedUser.getProviderProfile());
            providerCredits.setBalance(0);
            providerCreditsRepository.save(providerCredits);
        }

        log.info("Queueing welcome email for: {}", request.getEmail());
        String otpToken = NumberHelper.generateOtp();

        OTPToken generatedOtp = new OTPToken(otpToken, user, TokenType.REGISTER);
        generatedOtp.setOtpExpires(LocalDateTime.now().plusMinutes(5));
        otpTokenRepository.save(generatedOtp);

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setEmail(user.getEmail());
        emailRequest.setOtpToken(otpToken);
        emailRequest.setSubject("Welcome to Sewalo");
        emailRequest.setTemplatePath("/email/verify-account");

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitMQProducer.sendEmailNotification(emailRequest);
                log.info("Welcome email queued successfully for: {}", request.getEmail());
            }
        });

        UserResponseDto userResponseDto = new UserResponseDto(savedUser);

        Map<String, Object> res = new HashMap<>();
        res.put("access_token", accessToken);
        res.put("user", userResponseDto);
        log.info("Registered account");

        return APIResponse.success(res, "Successfully registered user.", Http_Constants.CREATED);
    }

    @Auditable(action = "LOGIN")
    public APIResponse authenticate(AuthRequestDto request, HttpServletResponse response) {
        log.debug("Attempting to authenticate user...");
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            log.warn("Authentication failed: Invalid credentials");
            throw new UsernameNotFoundException("Invalid credentials.");
        } catch (Exception e) {
            log.error("Authentication failed: {}", e.getMessage());
            throw e;
        }

        log.debug("Getting user details and checking domain records...");
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(request.getEmail());


        User user = userRepository.findUserByEmail(request.getEmail()).orElseThrow(() -> new ForbiddenException("Account not found."));
        user.setProvider(AuthProvider.LOCAL);


        log.debug("Generating secure access and refresh token pairs...");
        String accessToken = jwtUtils.generateToken(userDetails.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(userDetails.getUsername());
        LocalDateTime expiry = LocalDateTime.now().plusDays(7);

        refreshTokenRepository.save(new RefreshToken(refreshToken, expiry, user));

        log.debug("Configuring secure HttpOnly refresh token cookie wrapper...");
        ResponseCookie cookie = createCookie(refreshToken);
        setCookieHeader(cookie.toString(), response);

        Map<String, Object> data = new HashMap<>();
        data.put("access_token", accessToken);
        data.put("role", user.getRole());
        data.put("userId", user.getId());
        data.put("isActive", user.isActive());
        data.put("isOnboarded", user.isOnboarded());

        log.info("User authenticated. User id: {}", user.getId());

        return APIResponse.success(data, "Successfully authenticated user.", Http_Constants.OK);
    }

    @Auditable(action = "REFRESH_TOKEN")
    public APIResponse refreshToken(String token, HttpServletResponse response) {
        if (!jwtUtils.validateToken(token)) {
            throw new BadRequestException("Invalid refresh token");
        }

        RefreshToken refreshTokenFromDB = refreshTokenService.getRefreshToken(token);

        User user = refreshTokenFromDB.getUser();

        if (!user.isActive()) {
            throw new ForbiddenException("Inactive account.");
        }

        if (user.isAccountLocked()) {
            throw new ForbiddenException("Account locked.");
        }

        String username = refreshTokenFromDB.getUser().getEmail();
        String accessToken = jwtUtils.generateToken(username);
        String refreshToken = jwtUtils.generateRefreshToken(username);
        LocalDateTime expiry = LocalDateTime.now().plusDays(7);

        refreshTokenRepository.save(new RefreshToken(refreshToken, expiry, user));

        ResponseCookie cookie = createCookie(refreshToken);
        setCookieHeader(cookie.toString(), response);

        Map<String, Object> data = new HashMap<>();
        data.put("access_token", accessToken);
        data.put("role", user.getRole());
        data.put("userId", user.getId());
        data.put("isActive", user.isActive());

        return APIResponse.success(data, "Successfully authenticated user.", Http_Constants.OK);

    }

    @Auditable(action = "GOOGLE_AUTH")
    public APIResponse loginWithGoogle(GoogleLoginRequestDto request, HttpServletResponse response) {
        log.debug("Retrieving access token...");
        String acceeKey = request.getIdToken();

        RestTemplate restTemplate = new RestTemplate();
        String url = "https://www.googleapis.com/oauth2/v3/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(acceeKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> googleResponse;

        try {
            log.debug("Communicating with Google...");
            googleResponse = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            log.info("Successful Google response");
        } catch (Exception e) {
            log.error("Failed to get response from Google: {}", e.getMessage());
            throw new BadRequestException("Invalid Google token");
        }

        Map<String, Object> payload = googleResponse.getBody();

        String sub = (String) payload.get("sub");
        String email = (String) payload.get("email");
        String fullName = (String) payload.get("name");

        log.debug("Validating if user exits...");
        Optional<User> existing = userRepository.findByProviderId(sub);

        User user;

        if (existing.isPresent()) {
            user = existing.get();
            log.info("User exists!");
        } else {
            log.debug("Finding user with email...");
            Optional<User> foundUser = userRepository.findUserByEmail(email);
            if (foundUser.isPresent()) {
                log.info("User with email found!");
                user = foundUser.get();
                user.setProvider(AuthProvider.GOOGLE);
                user.setProviderId(sub);
                user.setActive(true);
                log.debug("Updating...");
                userRepository.save(user);
                log.info("Updated user!");
            } else {
                log.debug("Creating new user...");
                user = new User();
                user.setEmail(email);
                user.setFullName(fullName);
                user.setProvider(AuthProvider.GOOGLE);
                user.setProviderId(sub);
                user.setActive(true);
                user.setRole(UserRole.GUEST);
                user.setCreatedAt(LocalDateTime.now());
                userRepository.save(user);
                log.info("Created new user!");
            }
        }

        log.debug("Generating secure access and refresh token pairs...");
        String accessToken = jwtUtils.generateToken(user.getEmail());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());
        LocalDateTime expiry = LocalDateTime.now().plusDays(7);

        refreshTokenRepository.save(new RefreshToken(refreshToken, expiry, user));

        log.debug("Setting Http cookie...");
        ResponseCookie cookie = createCookie(refreshToken);
        setCookieHeader(cookie.toString(), response);

        Map<String, Object> data = new HashMap<>();
        data.put("access_token", accessToken);
        data.put("role", user.getRole());
        data.put("userId", user.getId());
        data.put("isActive", user.isActive());
        data.put("email", user.getEmail());
        data.put("isOnboarded", user.isOnboarded());
        log.info("Successfully authenticated with Google: {}", user.getEmail());

        return APIResponse.success(data, "Successfully authenticated user.", Http_Constants.OK);
    }

    @Auditable(action = "ACCOUNT_VERIFICATION")
    @Transactional
    public APIResponse accountVerification (String otpToken) {
        log.debug("Retrieving verification code...");
        OTPToken existingToken = otpTokenService.getOTPTokenByOtpToken(otpToken);

        if(existingToken.getOtpExpires().isBefore(LocalDateTime.now())) {
            otpTokenService.deleteOTPToken(existingToken);
            throw new BadRequestException("Expired OTP Token: Request a new OTP Token.");
        }

        User foundUser = userRepository.findById(existingToken.getUser().getId()).orElseThrow(() -> new BadRequestException("Account not found."));

        if(foundUser.isActive()) {
            otpTokenService.deleteOTPToken(existingToken);
            throw new BadRequestException("Account is already verified.");
        }

        foundUser.setActive(true);
        userRepository.save(foundUser);

        otpTokenService.deleteOTPToken(existingToken);

        return APIResponse.success(new UserResponseDto(foundUser), "Successfully Activated account.", Http_Constants.OK);
    }

    @Auditable(action = "RESEND_VERIFICATION_CODE")
    public APIResponse resendVerificationCode(UUID userId) {
        log.debug("Validating if user exits...");
        User foundUser = userRepository.findById(userId).orElseThrow(() -> new BadRequestException("Account not found."));

        log.debug("Retrieving existing verification code...");
       OTPToken existingToken = otpTokenService.getOTPTokenByUser(foundUser);

       if(existingToken != null) {
           log.debug("Deleting existing verification code...");
           otpTokenService.deleteOTPToken(existingToken);
       }

       log.debug("Creating verification code...");
       String generateOtp = NumberHelper.generateOtp();
       OTPToken otpToken = new OTPToken(generateOtp, foundUser, TokenType.GENERATION);

       otpTokenRepository.save(otpToken);
       log.info("Created verification code...");

       EmailRequest emailRequest = new EmailRequest();
       emailRequest.setEmail(foundUser.getEmail());
       emailRequest.setSubject("Verification Code");
       emailRequest.setOtpToken(generateOtp);
       emailRequest.setTemplatePath("/email/verificationCode.html");

        log.info("Sending verification code...");
//       TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//           @Override
//           public void afterCommit() {
               rabbitMQProducer.sendEmailNotification(emailRequest);
               log.info("Verification code resend email queued successfully for: {}", foundUser.getEmail());
//           }
//       });


       return APIResponse.success("Verification code resent successfully", Http_Constants.OK);
    }

    @Auditable(action = "UPDATE_PASSWORD")
    @Transactional
    public APIResponse updatePassword(PasswordUpdateRequestDto request, HttpServletResponse response) {

        if(!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match.");
        }

        String passwordValidation = PasswordValidation.validatePassword(request.getPassword());

        if(!passwordValidation.equals("Strong")) {
            throw new BadRequestException(passwordValidation);
        }

        User user = userRepository.findUserByEmail(request.getEmail()).orElseThrow(() -> new ResourceNotFoundException("Email not found."));

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        UserResponseDto res = new UserResponseDto(user);

        ResponseCookie cookie = ResponseCookie.from("Password-update", "changed")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .build();

        response.setHeader("Set-Cookie", cookie.toString());

        return APIResponse.success(res, "Successfully updated password.", Http_Constants.OK);
    }

    public APIResponse forgotPassword (EmailRequest req) {
        log.info("Processing forgot password for: {}", req.getEmail());

        User user = userRepository.findByEmail(req.getEmail()).orElseThrow(() -> new ResourceNotFoundException("No account found with that email."));

        String otpToken = NumberHelper.generateOtp();
        OTPToken generatedOtp = new OTPToken(otpToken, user, TokenType.PASSWORD_RESET);
        generatedOtp.setOtpExpires(LocalDateTime.now().plusMinutes(7));
        otpTokenRepository.save(generatedOtp);

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setEmail(req.getEmail());
        emailRequest.setSubject("Forgot Password");
        emailRequest.setOtpToken(otpToken);
        emailRequest.setTemplatePath("/email/forgot-password");

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitMQProducer.sendEmailNotification(emailRequest);
                log.info("Password reset email queued for: {}", req.getEmail());
            }
        });

        return APIResponse.success("Password reset email sent successfully.", Http_Constants.OK);
    }

    @Transactional
    public APIResponse completeGoogleAuth(GoogleOnboardingRequestDto req, String email, HttpServletResponse response) {
        log.debug("Finding user...");
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new UnauthorizedException("Email not found."));

        if(!user.getRole().equals(UserRole.GUEST)) {
            throw new BadRequestException("User is already authenticated!");
        }

        String requestedRole = req.getRole().toUpperCase();

        log.debug("Assigning user role...");
        if(requestedRole.equals(UserRole.PROVIDER.name())) {
            user.setRole(UserRole.PROVIDER);
            ProviderProfile providerProfile = new ProviderProfile();
            providerProfile.setUser(user);
            user.setProviderProfile(providerProfile);

        } else if(requestedRole.equals(UserRole.CUSTOMER.name())) {
            user.setRole(UserRole.CUSTOMER);
            CustomerProfile customerProfile = new CustomerProfile();
            customerProfile.setUser(user);
            user.setCustomerProfile(customerProfile);
        } else {
            throw new BadRequestException("Invalid user role. Only PROVIDER or CUSTOMER accepted.");
        }

        User savedUser = userRepository.save(user);
        log.info("Role assigned to user!");

        if (savedUser.getRole() == UserRole.PROVIDER) {
            ProviderCredits providerCredits = new ProviderCredits();
            providerCredits.setProvider(savedUser.getProviderProfile());
            providerCredits.setBalance(0);
            providerCreditsRepository.save(providerCredits); // now providerProfile.id is real
        }

        log.debug("Generating secure access and refresh token pairs...");
        String accessToken = jwtUtils.generateToken(user.getEmail());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());
        LocalDateTime expiry = LocalDateTime.now().plusDays(7);

        refreshTokenRepository.save(new RefreshToken(refreshToken, expiry, user));

        log.debug("Setting Http cookie...");
        ResponseCookie cookie = createCookie(refreshToken);
        setCookieHeader(cookie.toString(), response);

        Map<String, Object> data = new HashMap<>();
        data.put("access_token", accessToken);
        data.put("role", user.getRole());
        data.put("userId", user.getId());
        data.put("isActive", user.isActive());
        data.put("email", user.getEmail());
        data.put("isOnboarded", user.isOnboarded());
        log.info("Successfully set the user role.");

        return APIResponse.success(data, "Successfully set the user role.", Http_Constants.OK);
    }

    @Auditable(action = "USER_LOGOUT")
    @Transactional
    public APIResponse logout(String refreshToken, HttpServletResponse response) {
        log.debug("Logging out for current device...");

        // invalidate the specifice token in the DB
        if(refreshToken != null && !refreshToken.isBlank()) {
            try {

                RefreshToken token = refreshTokenService.getRefreshToken((refreshToken));
                refreshTokenRepository.delete(token);
                log.debug("Invalidated refresh token in DB.");
            } catch (Exception e) {
                log.warn("Refresh token could not be invalidated during logout.");
            }
        }

        // destroy the cookie on the client's browser
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth/refresh")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        setCookieHeader(cookie.toString(), response);
        log.info("Cleared refresh token cookie from browser.");

        return APIResponse.success(null, "Successfully logged out.", Http_Constants.OK);
    }

    private ResponseCookie createCookie(String token) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(false)
//                .secure(true)
                .path("/api/v1/auth/refresh")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
//                .sameSite("Strict")
                .build();
    }

    private void setCookieHeader(String cookie, HttpServletResponse response) {
        response.setHeader("Set-Cookie", cookie);
    }
}
