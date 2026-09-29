package com.lazorenkoo.urlshortener.controller;

import com.lazorenkoo.urlshortener.dto.UrlRequest;
import com.lazorenkoo.urlshortener.dto.UrlResponse;
import com.lazorenkoo.urlshortener.service.ShortUrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/shorten")
public class ShortUrlController {

	private final ShortUrlService service;

	public ShortUrlController(ShortUrlService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<UrlResponse> create(@RequestBody UrlRequest request) {
		UrlResponse response = UrlResponse.from(service.create(request.url()));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{shortCode}")
	public UrlResponse get(@PathVariable String shortCode) {
		return UrlResponse.from(service.get(shortCode));
	}

	@PutMapping("/{shortCode}")
	public UrlResponse update(@PathVariable String shortCode, @RequestBody UrlRequest request) {
		return UrlResponse.from(service.update(shortCode, request.url()));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, String>> handleError(ResponseStatusException exception) {
		return ResponseEntity.status(exception.getStatusCode())
				.body(Map.of("error", exception.getReason()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> handleInvalidJson() {
		return ResponseEntity.badRequest()
				.body(Map.of("error", "Request body must be JSON with a url field"));
	}
}
