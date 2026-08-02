package com.example.filtertest.service.matcher;

import java.time.Duration;
import java.util.regex.PatternSyntaxException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegexNameMatcherTest {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMillis(50);

    @Test
    void excludesNamesStartingWithE() {
        RegexNameMatcher matcher = RegexNameMatcher.compile("^E.*$", DEFAULT_TIMEOUT);

        assertThat(matcher.excludes("Eagle")).isTrue();
        assertThat(matcher.excludes("Falcon")).isFalse();
    }

    @Test
    void excludesNamesContainingEvaCharacters() {
        RegexNameMatcher matcher = RegexNameMatcher.compile("^.*[eva].*$", DEFAULT_TIMEOUT);

        assertThat(matcher.excludes("Ibis")).isFalse();
        assertThat(matcher.excludes("Vortex")).isTrue();
    }

    @Test
    void invalidPatternThrowsPatternSyntaxException() {
        assertThatThrownBy(() -> RegexNameMatcher.compile("(unclosed", DEFAULT_TIMEOUT))
                .isInstanceOf(PatternSyntaxException.class);
    }

    @Test
    void deadlineExceeded_excludesRowDefensively() {
        // Modern JDKs largely neutralize the textbook (a+)+ catastrophic-backtracking example, so
        // rather than depend on a real exponential-time pattern (fragile across JDK versions), we
        // force the deadline itself to have already expired and check that a match requiring more
        // than one deadline-check interval's worth of charAt() calls is caught and excluded.
        RegexNameMatcher matcher = RegexNameMatcher.compile("a*b", Duration.ofNanos(-1));
        String longNonMatchingInput = "a".repeat(500);

        assertThat(matcher.excludes(longNonMatchingInput)).isTrue();
    }
}
