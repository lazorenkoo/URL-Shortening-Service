package com.lazorenkoo.urlshortener.repository;

import com.lazorenkoo.urlshortener.model.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

	Optional<ShortUrl> findByShortCode(String shortCode);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update ShortUrl u set u.accessCount = u.accessCount + 1 where u.shortCode = :shortCode")
	int incrementAccessCount(@Param("shortCode") String shortCode);
}
