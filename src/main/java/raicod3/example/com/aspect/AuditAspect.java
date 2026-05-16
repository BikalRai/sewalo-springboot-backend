package raicod3.example.com.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import raicod3.example.com.annotation.Auditable;
import raicod3.example.com.dto.user.AuthRegistrationRequestDto;
import raicod3.example.com.dto.user.AuthRequestDto;
import raicod3.example.com.dto.user.PasswordUpdateRequestDto;
import raicod3.example.com.service.AuditLogService;
import raicod3.example.com.utilities.APIResponse;

import java.util.Map;

@Slf4j
@Aspect
@Component
public class AuditAspect {

    private final AuditLogService auditService;

    public AuditAspect(AuditLogService auditService) {
        this.auditService = auditService;
    }

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {

        String action = auditable.action();
        String ipAddress = getIpAddress();
        String userId = "ANONYMOUS";
        String userEmail = "ANONYMOUS";

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {
            userEmail = authentication.getName();
            userId = userEmail;
        } else {
            String extractedEmail = extractEmailFromArgs(joinPoint.getArgs());
            if (extractedEmail != null) {
                userEmail = extractedEmail;
                userId = extractedEmail;
            }
        }

        Object result;

        try {
            result = joinPoint.proceed();

            // After method runs — extract email from Google response
            if (action.equals("GOOGLE_AUTH") && result instanceof APIResponse apiResponse) {
                String googleEmail = extractEmailFromResponse(apiResponse);
                if (googleEmail != null) {
                    userEmail = googleEmail;
                    userId = googleEmail;
                }
            }

            auditService.log(userId, userEmail, action, "SUCCESS", ipAddress, action + " was successful");

        } catch (Throwable e) {
            auditService.log(userId, userEmail, action, "FAILED", ipAddress, e.getMessage());
            throw e;
        }

        return result;
    }

    private String extractEmailFromArgs(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof AuthRequestDto dto) {
                return dto.getEmail();
            }
            if (arg instanceof AuthRegistrationRequestDto dto) {
                return dto.getEmail();
            }
            if (arg instanceof PasswordUpdateRequestDto dto) {
                return dto.getEmail();
            }
        }
        return null;
    }

    private String getIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            log.error("Could not get IP address: {}", e.getMessage());
        }
        return "UNKNOWN";
    }

    private String extractEmailFromResponse(APIResponse apiResponse) {
        try {
            Object data = apiResponse.getData();
            if (data instanceof Map<?, ?> map) {
                String email = (String) map.get("email");
                if (email != null) return email;
            }
        } catch (Exception e) {
            log.error("Could not extract email from Google response: {}", e.getMessage());
        }
        return null;
    }
}
