package com.chavaillaz.appender.log4j.opensearch;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpensearchLog(
        LocalDateTime datetime,
        String host,
        String environment,
        String application,
        String logger,
        String level,
        String logmessage,
        String thread,
        String stacktrace,
        String key) {
}
