package raicod3.example.com.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import raicod3.example.com.model.AuditLog;
import raicod3.example.com.repository.AuditLogRepository;

import java.util.List;

@Service
@Slf4j
public class AuditLogService {
    private final AuditLogRepository auditRepository;

    public AuditLogService(AuditLogRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void log(String userId, String userEmail, String action, String status, String ipAddress, String details){
        try {
            log.debug("Creating audit log...");
            AuditLog auditLog = new AuditLog(userId, userEmail, action, status, ipAddress, details);
            auditRepository.save(auditLog);
            log.info("Audit log saved: {} - {} - {}", userId,  action, status);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }

    public List<AuditLog> getLogsByUser(String userId) {
        return auditRepository.findByUserId(userId);
    }

    public List<AuditLog> getLogsByAction(String action) {
        return auditRepository.findByAction(action);
    }

    public List<AuditLog> getLogsByStatus(String status) {
        return auditRepository.findByAction(status);
    }
}
