package com.example.filtertest.service.matcher;

import com.google.re2j.Pattern;

public final class Re2jNameMatcher implements NameMatcher {

    private final Pattern pattern;

    private Re2jNameMatcher(Pattern pattern) {
        this.pattern = pattern;
    }

    /**
     * @throws com.google.re2j.PatternSyntaxException if regex is invalid, or uses a construct
     *     RE2J doesn't support (e.g. backreferences, lookahead/lookbehind).
     */
    public static Re2jNameMatcher compile(String regex) {
        return new Re2jNameMatcher(Pattern.compile(regex));
    }

    @Override
    public boolean excludes(String name) {
        return pattern.matcher(name).matches();
    }
}
