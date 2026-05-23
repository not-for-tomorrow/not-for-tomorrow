package com.superapp.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SessionTokenRepository extends JpaRepository<SessionToken, Long> {
    Optional<SessionToken> findByRefreshToken(String refreshToken);
    List<SessionToken> findByUserAndRevokedFalse(UserAccount user);
    List<SessionToken> findByUserOrderByCreatedAtDesc(UserAccount user);
    Optional<SessionToken> findByIdAndUser(Long id, UserAccount user);

    @Modifying
    @Query("delete from SessionToken s where s.expiresAt < :time")
    int deleteExpiredBefore(Instant time);
}
