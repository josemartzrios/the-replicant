package com.thereplicant.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Service responsible for generating URL-friendly slugs from text.
 *
 * Slug rules:
 * - Lowercase
 * - Spaces and underscores replaced with hyphens
 * - Special characters removed
 * - Consecutive hyphens collapsed
 * - Unicode normalized (accents removed)
 * - Trimmed of leading/trailing hyphens
 * - Max length: 250 characters
 *
 * Security: Input is sanitized to prevent injection via slug (OWASP A03).
 */
@Slf4j
@Service
public class SlugService {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s_]+");
    private static final Pattern CONSECUTIVE_HYPHENS = Pattern.compile("-{2,}");
    private static final int MAX_SLUG_LENGTH = 250;

    /**
     * Generate a slug from the given text.
     *
     * @param text the input text (typically a post title or category name)
     * @return a URL-friendly slug
     * @throws IllegalArgumentException if text is null or blank
     */
    public String generateSlug(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text for slug generation cannot be null or blank");
        }

        String slug = text;

        // Normalize unicode characters (á → a, ñ → n, etc.)
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD);
        slug = slug.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

        // Convert to lowercase
        slug = slug.toLowerCase(Locale.ROOT);

        // Replace whitespace and underscores with hyphens
        slug = WHITESPACE.matcher(slug).replaceAll("-");

        // Remove non-latin characters (keeps letters, digits, hyphens)
        slug = NON_LATIN.matcher(slug).replaceAll("");

        // Collapse consecutive hyphens
        slug = CONSECUTIVE_HYPHENS.matcher(slug).replaceAll("-");

        // Trim leading/trailing hyphens
        slug = slug.replaceAll("^-+|-+$", "");

        // Enforce max length (truncate at last complete word if possible)
        if (slug.length() > MAX_SLUG_LENGTH) {
            slug = slug.substring(0, MAX_SLUG_LENGTH);
            int lastHyphen = slug.lastIndexOf('-');
            if (lastHyphen > MAX_SLUG_LENGTH / 2) {
                slug = slug.substring(0, lastHyphen);
            }
        }

        return slug;
    }

    /**
     * Generate a unique slug by appending a numeric suffix if conflicts exist.
     *
     * @param baseSlug the base slug
     * @param suffix   the numeric suffix to append
     * @return slug with suffix (e.g., "my-post-2")
     */
    public String appendSuffix(String baseSlug, int suffix) {
        return baseSlug + "-" + suffix;
    }
}
