package com.example.filtertest.service.matcher;

import com.google.re2j.PatternSyntaxException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Re2jNameMatcherTest {

    @Test
    void excludesNamesStartingWithE() {
        Re2jNameMatcher matcher = Re2jNameMatcher.compile("^E.*$");

        assertThat(matcher.excludes("Eagle")).isTrue();
        assertThat(matcher.excludes("Falcon")).isFalse();
    }

    @Test
    void excludesNamesContainingEvaCharacters() {
        Re2jNameMatcher matcher = Re2jNameMatcher.compile("^.*[eva].*$");

        assertThat(matcher.excludes("Ibis")).isFalse();
        assertThat(matcher.excludes("Vortex")).isTrue();
    }

    @Test
    void invalidPatternThrowsPatternSyntaxException() {
        assertThatThrownBy(() -> Re2jNameMatcher.compile("(unclosed"))
                .isInstanceOf(PatternSyntaxException.class);
    }

    @Test
    void backreferencesAreUnsupported() {
        assertThatThrownBy(() -> Re2jNameMatcher.compile("(a)\\1"))
                .isInstanceOf(PatternSyntaxException.class);
    }
}
