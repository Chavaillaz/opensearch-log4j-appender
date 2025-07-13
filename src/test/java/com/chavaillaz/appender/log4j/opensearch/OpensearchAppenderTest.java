package com.chavaillaz.appender.log4j.opensearch;

import static com.chavaillaz.appender.log4j.opensearch.OpensearchUtils.createClient;
import static java.net.InetAddress.getLocalHost;
import static java.time.Duration.ofSeconds;
import static org.apache.logging.log4j.Level.INFO;
import static org.apache.logging.log4j.LogManager.getRootLogger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testcontainers.shaded.org.apache.commons.lang3.ThreadUtils.sleep;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.testcontainers.utility.DockerImageName;

class OpensearchAppenderTest {

    // API Key generation is not yet supported and therefore not tested
    // https://github.com/opensearch-project/security/issues/1504

    public static final DockerImageName IMAGE = DockerImageName
            .parse("opensearchproject/opensearch")
            .withTag("3.1.0");

    protected static OpensearchAppender createAppender(String url, String username, String password) throws Exception {
        OpensearchAppender.Builder builder = OpensearchAppender.builder();
        builder.setName("my-appender");
        builder.setApplicationName("my-application");
        builder.setHostName(getLocalHost().getHostName());
        builder.setUrl(url);
        builder.setUser(username);
        builder.setPassword(password);
        builder.setFlushInterval(500);
        builder.setFlushThreshold(5);
        return builder.build();
    }

    protected static List<OpensearchLog> searchLog(OpenSearchClient client, String index, String id) throws IOException {
        return client.search(search -> search
                                .index(index + "*")
                                .query(query -> query
                                        .match(term -> term
                                                .field("logmessage")
                                                .query(value -> value.stringValue(id)))),
                        OpensearchLog.class)
                .hits()
                .hits()
                .stream()
                .map(Hit::source)
                .toList();
    }

    @Test
    void systemTestWithOpensearch() throws Exception {
        try (OpenSearchContainer<?> container = new OpenSearchContainer<>(IMAGE)) {
            container.start();

            // Given
            String id = UUID.randomUUID().toString();
            String logger = getRootLogger().getClass().getCanonicalName();
            OpenSearchClient client = createClient(container.getHttpHostAddress(), false, container.getUsername(), container.getPassword());
            OpensearchAppender appender = createAppender(container.getHttpHostAddress(), container.getUsername(), container.getPassword());
            ThreadContext.put("key", "value");

            // When
            appender.start();
            Log4jLogEvent event = Log4jLogEvent.newBuilder()
                    .setMessage(new SimpleMessage(id))
                    .setLoggerFqcn(logger)
                    .setThrown(new RuntimeException())
                    .setLevel(INFO)
                    .build();
            appender.append(event);
            sleep(ofSeconds(5));
            appender.stop();

            // Then
            List<OpensearchLog> logs = searchLog(client, appender.getLogConfiguration().getIndex(), id);
            assertEquals(1, logs.size());
            OpensearchLog log = logs.get(0);
            assertEquals(appender.getLogConfiguration().getHost(), log.getHost());
            assertEquals(appender.getLogConfiguration().getEnvironment(), log.getEnvironment());
            assertEquals(appender.getLogConfiguration().getApplication(), log.getApplication());
            assertEquals(logger, log.getLogger());
            assertEquals(INFO.toString(), log.getLevel());
            assertEquals(id, log.getLogmessage());
            assertEquals("value", log.getKey());
            assertNotNull(log.getStacktrace());
        }
    }

}
