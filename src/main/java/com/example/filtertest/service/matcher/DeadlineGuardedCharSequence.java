package com.example.filtertest.service.matcher;

final class DeadlineGuardedCharSequence implements CharSequence {

    // Amortizes System.nanoTime() cost across many charAt() calls instead of checking every call.
    private static final int CHECK_INTERVAL = 256;

    private final CharSequence delegate;
    private final long deadlineNanos;
    private int callsSinceCheck;

    DeadlineGuardedCharSequence(CharSequence delegate, long deadlineNanos) {
        this.delegate = delegate;
        this.deadlineNanos = deadlineNanos;
    }

    @Override
    public int length() {
        return delegate.length();
    }

    @Override
    public char charAt(int index) {
        if (++callsSinceCheck >= CHECK_INTERVAL) {
            callsSinceCheck = 0;
            if (System.nanoTime() >= deadlineNanos) {
                throw new MatchTimeoutException();
            }
        }
        return delegate.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new DeadlineGuardedCharSequence(delegate.subSequence(start, end), deadlineNanos);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
