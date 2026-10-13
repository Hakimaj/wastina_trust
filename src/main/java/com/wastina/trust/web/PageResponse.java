package com.wastina.trust.web;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * The single page envelope used by every paged endpoint.
 *
 * <p>The audit found list endpoints returning unbounded arrays with no way to page, sort or count.
 * Centralising the shape here means a frontend can write one response type and reuse it for
 * payments, guarantee requests, contracts, documents and compliance lists.
 *
 * @param content      the rows for the requested page
 * @param page         zero-based page index actually returned
 * @param size         page size actually applied, after clamping
 * @param totalElements total rows matching the query
 * @param totalPages   total number of pages
 * @param first        whether this is the first page
 * @param last         whether this is the last page
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public PageResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
        this(content, page, size, totalElements, totalPages, page == 0, totalPages <= 1 || page >= totalPages - 1);
    }

    /**
     * Builds a response from a Spring Data page of entities and a parallel list of mapped rows.
     *
     * <p>Separate type parameters because the page holds entities while the response holds views.
     */
    public static <E, T> PageResponse<T> of(Page<E> page, List<T> content) {
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    /** Builds a response for an in-memory window over a known total. */
    public static <T> PageResponse<T> of(List<T> window, int page, int size, long totalElements) {
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(
                window, page, size, totalElements, totalPages, page == 0, page >= totalPages - 1);
    }
}