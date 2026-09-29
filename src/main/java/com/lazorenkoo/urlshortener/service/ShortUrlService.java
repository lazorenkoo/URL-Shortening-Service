package com.lazorenkoo.urlshortener.service;

import com.lazorenkoo.urlshortener.model.ShortUrl;
import com.lazorenkoo.urlshortener.repository.ShortUrlRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;

@Service
public class ShortUrlService {

	private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private final SecureRandom random = new SecureRandom();
	private final ShortUrlRepository repository;

	public ShortUrlService(ShortUrlRepository repository) {
		this.repository = repository;
	}

	public ShortUrl create(String url) {
		validateUrl(url);

		for (int attempt = 0; attempt < 5; attempt++) {
			String code = generateCode();
			try {
				// Each save has its own transaction, so a duplicate can be retried.
				return repository.saveAndFlush(new ShortUrl(url, code));
			} catch (DataIntegrityViolationException exception) {
				if (repository.findByShortCode(code).isEmpty()) {
					throw exception;
				}
			}
		}
		throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
				"Could not generate a short code. Please try again.");
	}

	@Transactional
	public ShortUrl get(String shortCode) {
		if (repository.incrementAccessCount(shortCode) == 0) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Short URL not found");
		}
		return repository.findByShortCode(shortCode)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Short URL not found"));
	}

	@Transactional
	public ShortUrl update(String shortCode, String url) {
		validateUrl(url);
		ShortUrl shortUrl = repository.findByShortCode(shortCode)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Short URL not found"));
		shortUrl.setUrl(url);
		return repository.saveAndFlush(shortUrl);
	}

	private void validateUrl(String url) {
		if (url == null || url.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL is required");
		}
		try {
			URI uri = new URI(url);
			boolean isHttp = "http".equalsIgnoreCase(uri.getScheme())
					|| "https".equalsIgnoreCase(uri.getScheme());
			if (!isHttp || uri.getHost() == null || uri.getPort() > 65535) {
				throw new URISyntaxException(url, "Invalid HTTP URL");
			}
		} catch (URISyntaxException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"URL must be a valid http:// or https:// address");
		}
	}

	private String generateCode() {
		StringBuilder code = new StringBuilder();
		for (int i = 0; i < 6; i++) {
			code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
		}
		return code.toString();
	}
}
