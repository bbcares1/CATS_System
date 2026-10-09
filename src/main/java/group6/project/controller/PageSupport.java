package group6.project.controller;

import java.util.List;
import org.springframework.data.domain.*;

public final class PageSupport {
    // Shared bounds keep HTML lists consistent without another pagination framework.
    private PageSupport() {}

    // Clamp bookmarks and allow only the team's three supported page sizes.
    public static <T> Page<T> page(List<T> rows, int number, int size) {
        if (size != 10 && size != 20 && size != 25) size = 10;
        number = Math.max(0, Math.min(number, Math.max(0, (rows.size() - 1) / size)));
        int first = number * size;
        return new PageImpl<>(rows.subList(first, Math.min(first + size, rows.size())), PageRequest.of(number, size), rows.size());
    }
}
