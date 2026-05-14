package com.ekokoy.portal.user.service;

import com.ekokoy.portal.user.entity.AuditLog;
import com.ekokoy.portal.user.repository.AuditLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** Audit log kaydı oluşturur. Yeni transaction'da çalışır — asıl işlem rollback yapsa bile log yazılır. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID actorId, String action, String entityType, UUID entityId, String details) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }
}
