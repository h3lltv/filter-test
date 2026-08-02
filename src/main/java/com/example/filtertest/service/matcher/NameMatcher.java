package com.example.filtertest.service.matcher;

public interface NameMatcher {

    /**
     * @return true if the given name matches the configured pattern and must be excluded from results.
     */
    boolean excludes(String name);
}
