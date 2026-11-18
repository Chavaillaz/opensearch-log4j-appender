package com.chavaillaz.appender.log4j.opensearch;

import static com.chavaillaz.appender.log4j.opensearch.OpensearchApiKeyHelper.createApiKey;
import static com.chavaillaz.appender.log4j.opensearch.OpensearchUtils.createClient;
import static java.net.InetAddress.getLocalHost;
import static java.time.Duration.ofSeconds;
import static org.apache.logging.log4j.Level.INFO;
import static org.apache.logging.log4j.LogManager.getRootLogger;
import static org.assertj.core.api.Assertions.assertThat;

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
import org.testcontainers.utility.MountableFile;

class OpensearchAppenderTest {

    public static final boolean URL_TRUSTED = true;
    public static final DockerImageName IMAGE = DockerImageName
            .parse("opensearchproject/opensearch")
            .withTag("3.8.0");

    protected static OpensearchAppender createAppender(String url, String username, String password) throws Exception {
        OpensearchAppender.Builder builder = OpensearchAppender.builder();
        builder.setName("my-appender");
        builder.setApplication("my-application");
        builder.setHost(getLocalHost().getHostName());
        builder.setUrl(url);
        builder.setUrlTrusted(URL_TRUSTED);
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
            container.withSecurityEnabled();
            container.withCopyFileToContainer(
                    MountableFile.forClasspathResource("opensearch-security/config.yml"),
                    "/usr/share/opensearch/config/opensearch-security/config.yml"
            );
            container.start();

            // Given
            String id = UUID.randomUUID().toString();
            String logger = getRootLogger().getClass().getCanonicalName();
            OpenSearchClient client = new OpenSearchClient(createClient(container.getHttpHostAddress(), URL_TRUSTED, container.getUsername(), container.getPassword())._transport());
            OpenSearchClient apiClient = new OpenSearchClient(createClient(container.getHttpHostAddress(), URL_TRUSTED, createApiKey(client))._transport());
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
            Thread.sleep(ofSeconds(5));
            appender.stop();

            // Then
            List<OpensearchLog> logs = searchLog(apiClient, appender.getLogConfiguration().getIndex(), id);
            assertThat(logs).hasSize(1);
            OpensearchLog log = logs.get(0);
            assertThat(log.getHost()).isEqualTo(appender.getLogConfiguration().getHost());
            assertThat(log.getEnvironment()).isEqualTo(appender.getLogConfiguration().getEnvironment());
            assertThat(log.getApplication()).isEqualTo(appender.getLogConfiguration().getApplication());
            assertThat(log.getLogger()).isEqualTo(logger);
            assertThat(log.getLevel()).isEqualTo(INFO.toString());
            assertThat(log.getLogmessage()).isEqualTo(id);
            assertThat(log.getKey()).isEqualTo("value");
            assertThat(log.getStacktrace()).isNotNull();
        }
    }

}
