package com.lazorenkoo.urlshortener;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
class UrlShortenerApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void connectsToDatabase() {
		Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
		assertEquals(1, result);
	}

}
