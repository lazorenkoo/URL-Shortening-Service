package com.lazorenkoo.urlshortener.controller;

import com.lazorenkoo.urlshortener.service.ShortUrlService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RedirectController {

	private final ShortUrlService service;

	public RedirectController(ShortUrlService service) {
		this.service = service;
	}

	@GetMapping("/s/{shortCode}")
	public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
		String url = service.get(shortCode).getUrl();
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(url))
				.cacheControl(CacheControl.noStore())
				.build();
	}
}
