package com.lazorenkoo.urlshortener.dto;

import com.lazorenkoo.urlshortener.model.ShortUrl;

import java.time.Instant;

public record UrlStatsResponse(String id, String url, String shortCode,
		Instant createdAt, Instant updatedAt, long accessCount) {

	public static UrlStatsResponse from(ShortUrl shortUrl) {
		return new UrlStatsResponse(shortUrl.getId().toString(), shortUrl.getUrl(),
				shortUrl.getShortCode(), shortUrl.getCreatedAt(), shortUrl.getUpdatedAt(),
				shortUrl.getAccessCount());
	}
}
