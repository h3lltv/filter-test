package com.example.filtertest.service.matcher;

final class MatchTimeoutException extends RuntimeException {

    MatchTimeoutException() {
        // no message/cause/suppression/stack trace: this is thrown as a routine control-flow
        // signal on the hot matching path, so skipping fillInStackTrace() avoids its cost.
        super(null, null, false, false);
    }
}
