package com.guru.erp.platform.web;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Server-driven pagination envelope (ARCHITECTURE.md §3.2 — lists are
 * server-paged: {@code ?page&size&sort&filter}). Frontend never filters or
 * slices whole lists client-side.
 *
 * @param content    the rows for this page (already joined DTOs)
 * @param page       zero-based page index
 * @param size       page size requested
 * @param totalElements total matching rows across all pages
 * @param totalPages number of pages
 */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    /** Wrap a Spring Data {@link Page}, mapping each entity to a DTO. */
    public static <E, D> PageResponse<D> of(Page<E> page, Function<E, D> mapper) {
        return new PageResponse<>(
            page.getContent().stream().map(mapper).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages());
    }

    /** Wrap a page whose content is already the target type. */
    public static <T> PageResponse<T> of(Page<T> page) {
        return of(page, Function.identity());
    }
}
