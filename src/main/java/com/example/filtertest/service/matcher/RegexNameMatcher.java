package com.example.filtertest.service.matcher;

import java.time.Duration;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RegexNameMatcher implements NameMatcher {

    private static final Logger log = LoggerFactory.getLogger(RegexNameMatcher.class);
    private static final int LOGGED_TIMEOUTS_BEFORE_SAMPLING = 5;

    private final Pattern pattern;
    private final long budgetNanos;
    private int timeoutCount;

    private RegexNameMatcher(Pattern pattern, Duration matchTimeout) {
        this.pattern = pattern;
        this.budgetNanos = matchTimeout.toNanos();
    }

    /**
     * @throws java.util.regex.PatternSyntaxException if regex is not a valid Java regular expression.
     */
    public static RegexNameMatcher compile(String regex, Duration matchTimeout) {
        return new RegexNameMatcher(Pattern.compile(regex), matchTimeout);
    }

    @Override
    public boolean excludes(String name) {
        long deadline = System.nanoTime() + budgetNanos;
        CharSequence guarded = new DeadlineGuardedCharSequence(name, deadline);
        try {
            return pattern.matcher(guarded).matches();
        } catch (MatchTimeoutException e) {
            // A fresh matcher is compiled per HTTP request (see ProductFilterService), so this field
            // is never touched by more than one request; no synchronization needed.
            timeoutCount++;
            if (timeoutCount <= LOGGED_TIMEOUTS_BEFORE_SAMPLING || timeoutCount % 100 == 0) {
                log.warn("Regex match against pattern '{}' exceeded the {}ns per-match deadline "
                                + "(occurrence #{} for this request); excluding the row defensively",
                        pattern.pattern(), budgetNanos, timeoutCount);
            }
            return true;
        }
    }
}
