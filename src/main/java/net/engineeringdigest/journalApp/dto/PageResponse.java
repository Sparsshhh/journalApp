package net.engineeringdigest.journalApp.dto;

import lombok.Value;

import java.util.List;

// One page of results. "page" starts at 0.
@Value
public class PageResponse<T> {
    List<T> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
}
