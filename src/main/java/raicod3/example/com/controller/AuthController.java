package raicod3.example.com.controller;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import raicod3.example.com.dto.email.EmailRequest;
import raicod3.example.com.dto.google.GoogleLoginRequestDto;
import raicod3.example.com.dto.otp.OtpRquestDto;
import raicod3.example.com.dto.user.AuthRegistrationRequestDto;
import raicod3.example.com.dto.user.AuthRequestDto;
import raicod3.example.com.dto.user.OTPResendDto;
import raicod3.example.com.dto.user.PasswordUpdateRequestDto;
import raicod3.example.com.service.AuthService;
import raicod3.example.com.utilities.APIResponse;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<APIResponse> register(@RequestBody AuthRegistrationRequestDto request, HttpServletResponse response) throws MessagingException {
        APIResponse res = authService.registerUser(request, response);

        return new ResponseEntity<>(res,HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<APIResponse> login(@RequestBody AuthRequestDto request, HttpServletResponse res) {
        log.info("Login request: {}", request.getEmail());
        APIResponse response = authService.authenticate(request, res);
        log.info("Login response: {}", response);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<APIResponse> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken, HttpServletResponse response) {

       APIResponse res =  authService.refreshToken(refreshToken, response);

        return ResponseEntity.ok(res);
    }

    @PostMapping("/google")
    public ResponseEntity<APIResponse> googleLogin(@RequestBody GoogleLoginRequestDto request, HttpServletResponse response) {
        log.info("Attempting to authenticate with google");
        APIResponse res = authService.loginWithGoogle(request, response);
        return new ResponseEntity<>(res,HttpStatus.OK);
    }

    @PostMapping("verify-account")
    public ResponseEntity<APIResponse> verifyAccount(@RequestBody OtpRquestDto reqDto) {
        log.info("Verify account token: {}", reqDto.getOtpToken());
        APIResponse res = authService.accountVerification(reqDto.getOtpToken());
        return new ResponseEntity<>(res,HttpStatus.OK);
    }

    @PostMapping("/resend-code")
    public ResponseEntity<APIResponse> resendCode(@RequestBody OTPResendDto dto) {
        UUID idUUID = UUID.fromString(dto.getUserId());
        APIResponse res = authService.resendVerificationCode(idUUID);
        return new ResponseEntity<>(res,HttpStatus.CREATED);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<APIResponse> forgotPassword(@RequestBody EmailRequest req) {
        log.info("Attempting to reset password for: {}", req.getEmail());

        APIResponse res = authService.forgotPassword(req);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<APIResponse> updatePassword(@RequestBody PasswordUpdateRequestDto request, HttpServletResponse response) throws MessagingException {
        log.info("Attempting to update password: {}", request);

        APIResponse res = authService.updatePassword(request, response);

        return ResponseEntity.ok(res);
    }

}