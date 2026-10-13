package br.com.asobi.twofactor.challenge;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;

public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {

	/** Trava a linha: duas tentativas ao mesmo tempo não podem gastar uma tentativa só. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT c FROM Challenge c WHERE c.id = :id")
	Optional<Challenge> findByIdForUpdate(@Param("id") UUID id);

	long countByEmailAndCreatedAtAfter(String email, Instant since);

	/** Pedir um código novo aposenta os anteriores do mesmo e-mail e finalidade. */
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			UPDATE Challenge c SET c.expiresAt = :now
			WHERE c.email = :email AND c.purpose = :purpose AND c.id <> :keepId
			  AND c.consumedAt IS NULL AND c.expiresAt > :now
			""")
	int expireOthers(@Param("email") String email, @Param("purpose") String purpose,
			@Param("keepId") UUID keepId, @Param("now") Instant now);

	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("DELETE FROM Challenge c WHERE c.expiresAt < :before")
	int deleteExpiredBefore(@Param("before") Instant before);
}
