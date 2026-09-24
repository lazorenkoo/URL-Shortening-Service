package com.lazorenkoo.urlshortener;

import com.jayway.jsonpath.JsonPath;
import com.lazorenkoo.urlshortener.model.ShortUrl;
import com.lazorenkoo.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
