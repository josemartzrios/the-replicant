package com.thereplicant.api.dto.common;

import lombok.*;

import java.time.Instant;

/**
 * Pagination metadata for list/paginated responses.
 * Matches the PaginationMeta schema in api-spec.yaml.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationMeta {

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
