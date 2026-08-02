package com.example.filtertest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
class MatcherSchedulerConfig {

    // A dedicated pool for name-matching work, isolated from Reactor's shared, JVM-wide
    // Schedulers.parallel(), so concurrent filtering requests don't contend with unrelated
    // CPU-bound work elsewhere in the app for the same fixed-size thread pool.
    @Bean(destroyMethod = "dispose")
    Scheduler matcherScheduler() {
        return Schedulers.newParallel("name-matcher", Runtime.getRuntime().availableProcessors());
    }
}
