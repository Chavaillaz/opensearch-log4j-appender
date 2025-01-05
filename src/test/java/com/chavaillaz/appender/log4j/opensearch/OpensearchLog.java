package com.chavaillaz.appender.log4j.opensearch;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpensearchLog {

    private LocalDateTime datetime;
    private String host;
    private String environment;
    private String application;
    private String logger;
    private String level;
    private String logmessage;
    private String thread;
    private String stacktrace;
    private String key;

}