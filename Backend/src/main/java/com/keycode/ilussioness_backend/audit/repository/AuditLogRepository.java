package com.keycode.ilussioness_backend.audit.repository;

import com.keycode.ilussioness_backend.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Insert-oriented repository for audit records.
 *
 * <p>It extends {@link PagingAndSortingRepository} to support paginated reads
 * without exposing {@code delete/update} operations. Specifications are used
 * for dynamic filtering.</p>
 */
@org.springframework.stereotype.Repository
public interface AuditLogRepository extends Repository<AuditLog, UUID>,
        PagingAndSortingRepository<AuditLog, UUID>,
        JpaSpecificationExecutor<AuditLog> {

    <S extends AuditLog> S save(S entity);

    Optional<AuditLog> findById(UUID id);

    long count();
}
