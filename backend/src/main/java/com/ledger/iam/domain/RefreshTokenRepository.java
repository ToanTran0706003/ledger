package com.ledger.iam.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenRecord, UUID> {

    /**
     * Tiêu thụ NGUYÊN TỬ một jti: xoá hàng và trả số dòng (1 = hợp lệ & vừa thu hồi, 0 = không tồn
     * tại -> token đã dùng/đã thu hồi). Bảo đảm refresh single-use + phát hiện tái sử dụng.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshTokenRecord r WHERE r.id = :id")
    int consume(@Param("id") UUID id);

    /** Thu hồi mọi refresh token của một user (logout). */
    @Modifying
    @Transactional
    void deleteByUserId(UUID userId);
}
