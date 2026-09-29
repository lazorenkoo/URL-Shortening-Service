package com.lazorenkoo.urlshortener;

import com.jayway.jsonpath.JsonPath;
import com.lazorenkoo.urlshortener.model.ShortUrl;
import com.lazorenkoo.urlshortener.repository.ShortUrlRepository;
import com.lazorenkoo.urlshortener.service.ShortUrlService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@AutoConfigureMockMvc
@Transactional
class ShortUrlControllerTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ShortUrlRepository repository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private ShortUrlService service;

	@Test
	void redirectsAndCountsEachVisit() throws Exception {
		String code = UUID.randomUUID().toString();
		repository.saveAndFlush(new ShortUrl("https://example.com/old", code));
		for (int count = 1; count <= 2; count++) {
			mvc.perform(get("/s/{shortCode}", code))
					.andExpect(status().isFound())
					.andExpect(header().string("Location", "https://example.com/old"))
					.andExpect(header().string("Cache-Control", "no-store"));
			assertEquals(count, repository.findByShortCode(code).orElseThrow().getAccessCount());
		}
		service.update(code, "https://example.com/new");
		mvc.perform(get("/s/{shortCode}", code))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.com/new"));
		assertEquals(3, repository.findByShortCode(code).orElseThrow().getAccessCount());
		service.delete(code);
		mvc.perform(get("/s/{shortCode}", code)).andExpect(status().isNotFound());
	}

	@Test
	void getsUrlAndCountsEachAccess() throws Exception {
		String code = UUID.randomUUID().toString();
		ShortUrl saved = repository.saveAndFlush(new ShortUrl("https://example.com/path", code));
		entityManager.refresh(saved);
		Instant createdAt = saved.getCreatedAt();
		Instant updatedAt = saved.getUpdatedAt();

		for (int count = 1; count <= 2; count++) {
			mvc.perform(get("/shorten/{shortCode}", code))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(saved.getId().toString()))
					.andExpect(jsonPath("$.url").value(saved.getUrl()))
					.andExpect(jsonPath("$.shortCode").value(code))
					.andExpect(jsonPath("$.createdAt").value(createdAt.toString()))
					.andExpect(jsonPath("$.updatedAt").value(updatedAt.toString()))
					.andExpect(jsonPath("$.accessCount").doesNotExist());
			entityManager.clear();
			assertEquals(count, repository.findByShortCode(code).orElseThrow().getAccessCount());
		}
	}

	@Test
	void returns404ForUnknownCode() throws Exception {
		long count = repository.count();
		mvc.perform(get("/shorten/{shortCode}", UUID.randomUUID().toString()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Short URL not found"));
		assertEquals(count, repository.count());
	}

	@Test
	void getsStatsWithoutCountingStatsRequests() throws Exception {
		String code = UUID.randomUUID().toString();
		ShortUrl saved = repository.saveAndFlush(new ShortUrl("https://example.com", code));
		entityManager.refresh(saved);
		Instant createdAt = saved.getCreatedAt();
		Instant updatedAt = saved.getUpdatedAt();

		mvc.perform(get("/shorten/{shortCode}/stats", code))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessCount").value(0));
		mvc.perform(get("/shorten/{shortCode}", code)).andExpect(status().isOk());
		mvc.perform(get("/shorten/{shortCode}", code)).andExpect(status().isOk());

		for (int i = 0; i < 2; i++) {
			mvc.perform(get("/shorten/{shortCode}/stats", code))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(saved.getId().toString()))
					.andExpect(jsonPath("$.url").value(saved.getUrl()))
					.andExpect(jsonPath("$.shortCode").value(code))
					.andExpect(jsonPath("$.createdAt").value(createdAt.toString()))
					.andExpect(jsonPath("$.updatedAt").value(updatedAt.toString()))
					.andExpect(jsonPath("$.accessCount").value(2));
		}
		entityManager.clear();
		assertEquals(2, repository.findByShortCode(code).orElseThrow().getAccessCount());
	}

	@Test
	void returns404ForUnknownStatsCode() throws Exception {
		mvc.perform(get("/shorten/{shortCode}/stats", UUID.randomUUID().toString()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Short URL not found"));
	}

	@Test
	void deletesUrl() throws Exception {
		String code = UUID.randomUUID().toString();
		repository.saveAndFlush(new ShortUrl("https://example.com", code));

		mvc.perform(delete("/shorten/{shortCode}", code))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		entityManager.flush();
		entityManager.clear();
		assertTrue(repository.findByShortCode(code).isEmpty());
		mvc.perform(get("/shorten/{shortCode}", code))
				.andExpect(status().isNotFound());
		mvc.perform(delete("/shorten/{shortCode}", code))
				.andExpect(status().isNotFound());
	}

	@Test
	void returns404WhenDeletingUnknownCode() throws Exception {
		long count = repository.count();
		mvc.perform(delete("/shorten/{shortCode}", UUID.randomUUID().toString()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Short URL not found"));
		assertEquals(count, repository.count());
	}

	@Test
	void updatesUrlWithoutChangingCodeOrAccessCount() throws Exception {
		String code = UUID.randomUUID().toString();
		ShortUrl saved = repository.saveAndFlush(new ShortUrl("https://example.com/old", code));
		entityManager.refresh(saved);
		Instant createdAt = saved.getCreatedAt();
		Instant updatedAt = saved.getUpdatedAt();
		service.get(code);

		mvc.perform(put("/shorten/{shortCode}", code)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"https://example.com/new\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(saved.getId().toString()))
				.andExpect(jsonPath("$.url").value("https://example.com/new"))
				.andExpect(jsonPath("$.shortCode").value(code))
				.andExpect(jsonPath("$.createdAt").value(createdAt.toString()))
				.andExpect(jsonPath("$.accessCount").doesNotExist());

		entityManager.clear();
		ShortUrl updated = repository.findByShortCode(code).orElseThrow();
		assertEquals("https://example.com/new", updated.getUrl());
		assertEquals(1, updated.getAccessCount());
		assertTrue(updated.getUpdatedAt().isAfter(updatedAt));
	}

	@Test
	void returns404WhenUpdatingUnknownCode() throws Exception {
		mvc.perform(put("/shorten/{shortCode}", UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"https://example.com/new\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Short URL not found"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"url\":null}", "{\"url\":\"\"}",
			"{\"url\":\"example.com\"}", "{\"url\":\"ftp://example.com\"}", "{", "", "null", "[]"})
	void rejectsInvalidUpdate(String body) throws Exception {
		String code = UUID.randomUUID().toString();
		ShortUrl saved = repository.saveAndFlush(new ShortUrl("https://example.com/old", code));
		entityManager.refresh(saved);
		Instant updatedAt = saved.getUpdatedAt();

		mvc.perform(put("/shorten/{shortCode}", code)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").isNotEmpty());

		entityManager.clear();
		ShortUrl unchanged = repository.findByShortCode(code).orElseThrow();
		assertEquals("https://example.com/old", unchanged.getUrl());
		assertEquals(updatedAt, unchanged.getUpdatedAt());
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void countsConcurrentAccesses() throws Exception {
		String code = UUID.randomUUID().toString();
		ShortUrl saved = repository.saveAndFlush(new ShortUrl("https://example.com/concurrent", code));
		var executor = Executors.newFixedThreadPool(8);
		try {
			var requests = new ArrayList<Callable<ShortUrl>>();
			for (int i = 0; i < 20; i++) {
				requests.add(() -> service.get(code));
			}
			for (var result : executor.invokeAll(requests, 15, TimeUnit.SECONDS)) {
				assertEquals(saved.getId(), result.get().getId());
			}
			assertEquals(20, repository.findByShortCode(code).orElseThrow().getAccessCount());
		} finally {
			executor.shutdownNow();
			executor.awaitTermination(10, TimeUnit.SECONDS);
			repository.deleteById(saved.getId());
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"https://example.com/path?q=java#part", "http://example.com/path"})
	void createsAndStoresUrl(String url) throws Exception {
		String json = mvc.perform(post("/shorten")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"" + url + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isString())
				.andExpect(jsonPath("$.url").value(url))
				.andExpect(jsonPath("$.createdAt").isString())
				.andExpect(jsonPath("$.updatedAt").isString())
				.andExpect(jsonPath("$.accessCount").doesNotExist())
				.andReturn().getResponse().getContentAsString();

		String code = JsonPath.read(json, "$.shortCode");
		assertTrue(code.matches("[a-zA-Z0-9]{6}"));
		ShortUrl saved = repository.findByShortCode(code).orElseThrow();
		assertEquals(url, saved.getUrl());
		assertEquals(0, saved.getAccessCount());
		assertEquals(saved.getId().toString(), JsonPath.read(json, "$.id"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"url\":null}", "{\"url\":\"\"}",
			"{\"url\":\"   \"}", "{\"url\":\"example.com\"}",
			"{\"url\":\"ftp://example.com/file\"}", "{\"url\":\"https://\"}",
			"{\"url\":\"https://example.com/a b\"}",
			"{\"url\":\"https://example.com:99999\"}", "{", "", "null", "[]"})
	void rejectsInvalidRequest(String body) throws Exception {
		long count = repository.count();
		mvc.perform(post("/shorten").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").isNotEmpty());
		assertEquals(count, repository.count());
	}
}
