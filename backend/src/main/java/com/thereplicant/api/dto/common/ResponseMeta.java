package com.thereplicant.api.dto.common;

import lombok.*;

import java.time.Instant;

/**
 * Simple metadata wrapper for single-resource responses.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseMeta {

    @Builder.Default
    private Instant timestamp = Instant.now();
}
