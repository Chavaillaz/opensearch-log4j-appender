package com.chavaillaz.appender.log4j.opensearch;

import static com.chavaillaz.appender.log4j.opensearch.OpensearchUtils.createClient;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;

class OpensearchUtilsTest {

    @Test
    void testCreateClientWithoutCredentials() throws Exception {
        // Given: a cluster without security, reached with neither a user nor an API key
        OpensearchConfiguration configuration = OpensearchConfiguration.builder()
                .url("http://localhost:9200")
                .build();

        // When
        OpenSearchAsyncClient client = createClient(configuration);

        // Then
        assertThat(client).isNotNull();
        client._transport().close();
    }

    @Test
    void testCreateClientWithoutPassword() throws Exception {
        // Given: a user whose password is left empty
        OpensearchConfiguration configuration = OpensearchConfiguration.builder()
                .url("http://localhost:9200")
                .user("admin")
                .build();

        // When
        OpenSearchAsyncClient client = createClient(configuration);

        // Then
        assertThat(client).isNotNull();
        client._transport().close();
    }

}
