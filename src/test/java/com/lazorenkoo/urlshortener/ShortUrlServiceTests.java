package com.lazorenkoo.urlshortener;

import com.lazorenkoo.urlshortener.model.ShortUrl;
import com.lazorenkoo.urlshortener.repository.ShortUrlRepository;
import com.lazorenkoo.urlshortener.service.ShortUrlService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShortUrlServiceTests {

	@Test
	void retriesWhenCodeAlreadyExists() {
		ShortUrlRepository repository = mock(ShortUrlRepository.class);
		ShortUrl saved = new ShortUrl("https://example.com", "abc123");
		when(repository.saveAndFlush(any(ShortUrl.class)))
				.thenThrow(new DataIntegrityViolationException("Duplicate code"))
				.thenReturn(saved);
		when(repository.findByShortCode(anyString())).thenReturn(Optional.of(saved));

		assertSame(saved, new ShortUrlService(repository).create("https://example.com"));
		verify(repository, times(2)).saveAndFlush(any(ShortUrl.class));
	}

	@Test
	void doesNotRetryOtherDatabaseErrors() {
		ShortUrlRepository repository = mock(ShortUrlRepository.class);
		when(repository.saveAndFlush(any(ShortUrl.class)))
				.thenThrow(new DataIntegrityViolationException("Other constraint"));
		when(repository.findByShortCode(anyString())).thenReturn(Optional.empty());

		assertThrows(DataIntegrityViolationException.class,
				() -> new ShortUrlService(repository).create("https://example.com"));
		verify(repository).saveAndFlush(any(ShortUrl.class));
	}
}
