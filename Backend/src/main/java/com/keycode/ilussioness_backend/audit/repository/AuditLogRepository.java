package com.keycode.ilussioness_backend.audit.repository;

import com.keycode.ilussioness_backend.audit.domain.AuditLog;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Insert-oriented repository for audit records.
 *
 * <p>It intentionally extends the marker {@link Repository} instead of
 * {@code JpaRepository}/{@code CrudRepository} so delete/update methods are
 * not exposed to application code.</p>
 */
@org.springframework.stereotype.Repository
public interface AuditLogRepository extends Repository<AuditLog, UUID> {

    <S extends AuditLog> S save(S entity);

    Optional<AuditLog> findById(UUID id);

    long count();
}
