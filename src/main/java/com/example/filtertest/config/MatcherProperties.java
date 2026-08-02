package com.example.filtertest.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("shop.matcher.regex")
public record MatcherProperties(@DefaultValue("50ms") Duration matchTimeout) {
}
