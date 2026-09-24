package com.lazorenkoo.urlshortener.repository;

import com.lazorenkoo.urlshortener.model.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

	Optional<ShortUrl> findByShortCode(String shortCode);
}
