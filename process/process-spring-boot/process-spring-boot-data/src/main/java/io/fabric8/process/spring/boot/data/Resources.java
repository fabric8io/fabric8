package io.fabric8.process.spring.boot.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.collect.Iterables.filter;
import static java.util.Arrays.asList;

public class Resources {

    private final Link[] links;

    private final String[] content;

    private final Page page;

    @JsonCreator
    public Resources(@JsonProperty("links") Link[] links, @JsonProperty("content") String[] content, @JsonProperty("page") Page page) {
        this.links = links;
        this.content = content;
        this.page = page;
    }

    public List<Link> links() {
        return ImmutableList.copyOf(filter(asList(links), new Predicate<Link>() {
            @Override
            public boolean apply(Link input) {
                return !input.rel().equals("self");
            }
        }));
    }

    public String[] getContent() {
        return content;
    }

    public Page getPage() {
        return page;
    }

    public static class Link {

        private final String rel;

        private final String href;

        @JsonCreator
        public Link(@JsonProperty("rel") String rel, @JsonProperty("href") String href) {
            this.rel = rel;
            this.href = href;
        }

        public String rel() {
            return rel;
        }

        public String href() {
            return href;
        }

    }

    public static class Page {

        private long size;
        private long totalElements;
        private long totalPages;
        private long number;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }

        public long getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(long totalPages) {
            this.totalPages = totalPages;
        }

        public long getNumber() {
            return number;
        }

        public void setNumber(long number) {
            this.number = number;
        }
    }

}