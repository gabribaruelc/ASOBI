package br.com.asobi.notification.subscription;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

	Optional<Subscription> findByEmailAndProductIdAndType(String email, Long productId, AlertType type);

	Optional<Subscription> findByUnsubscribeToken(String unsubscribeToken);

	List<Subscription> findByProductIdAndType(Long productId, AlertType type);

	List<Subscription> findAllByOrderByConsentedAtDesc();

	List<Subscription> findByEmailOrderByConsentedAtDesc(String email);

	List<Subscription> findByProductIdOrderByConsentedAtDesc(Long productId);

	List<Subscription> findByEmailAndProductIdOrderByConsentedAtDesc(String email, Long productId);

	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("DELETE FROM Subscription s WHERE s.email = :email")
	int deleteAllByEmail(@Param("email") String email);

	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("DELETE FROM Subscription s WHERE s.productId = :productId")
	int deleteAllByProductId(@Param("productId") Long productId);

	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("UPDATE Subscription s SET s.lastNotifiedAt = :now WHERE s.id IN :ids")
	int markNotified(@Param("ids") Collection<Long> ids, @Param("now") Instant now);
}
