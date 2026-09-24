package com.lazorenkoo.urlshortener.dto;

import com.lazorenkoo.urlshortener.model.ShortUrl;

import java.time.Instant;

public record UrlResponse(String id, String url, String shortCode,
		Instant createdAt, Instant updatedAt) {

	public static UrlResponse from(ShortUrl shortUrl) {
		return new UrlResponse(shortUrl.getId().toString(), shortUrl.getUrl(),
				shortUrl.getShortCode(), shortUrl.getCreatedAt(), shortUrl.getUpdatedAt());
	}
}
