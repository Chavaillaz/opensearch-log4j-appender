package com.chavaillaz.appender.log4j.opensearch;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

/**
 * OpenSearch specific utility methods.
 */
@UtilityClass
public class OpensearchUtils {

    /**
     * Creates a new OpenSearch client.
     *
     * @param configuration The configuration to use
     * @return The OpenSearch client with the given configuration
     */
    public static OpenSearchClient createClient(OpensearchConfiguration configuration) {
        if (isNotBlank(configuration.getApiKey())) {
            return createClient(configuration.getUrl(), configuration.getApiKey());
        } else {
            return createClient(configuration.getUrl(), configuration.getUser(), configuration.getPassword());
        }
    }

    /**
     * Creates a new OpenSearch client.
     *
     * @param url      The URL of the OpenSearch instance to reach
     * @param username The username to authenticate
     * @param password The password corresponding to the given username
     * @return The OpenSearch client with the given configuration
     */
    @SneakyThrows
    public static OpenSearchClient createClient(String url, String username, String password) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(null, -1),
                new UsernamePasswordCredentials(username, password.toCharArray())
        );

        JacksonJsonpMapper jsonMapper = new JacksonJsonpMapper();
        jsonMapper.objectMapper().registerModule(new JavaTimeModule());

        return new OpenSearchClient(ApacheHttpClient5TransportBuilder
                .builder(HttpHost.create(url))
                .setMapper(jsonMapper)
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider))
                .build());
    }

    /**
     * Creates a new OpenSearch client.
     *
     * @param url    The URL of the OpenSearch instance to reach
     * @param apiKey The encoded API key to authenticate
     * @return The OpenSearch client with the given configuration
     */
    @SneakyThrows
    public static OpenSearchClient createClient(String url, String apiKey) {
        Header headerApiKey = new BasicHeader("Authorization", "ApiKey " + apiKey);

        JacksonJsonpMapper jsonMapper = new JacksonJsonpMapper();
        jsonMapper.objectMapper().registerModule(new JavaTimeModule());

        return new OpenSearchClient(ApacheHttpClient5TransportBuilder
                .builder(HttpHost.create(url))
                .setMapper(jsonMapper)
                .setDefaultHeaders(new Header[]{headerApiKey})
                .build());
    }

}
