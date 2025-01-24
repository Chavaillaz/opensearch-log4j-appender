package com.chavaillaz.appender.log4j.opensearch;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.http.auth.AuthScope.ANY;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javax.net.ssl.SSLContext;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;

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
            return createClient(configuration.getUrl(), createPermissiveContext(), configuration.getApiKey());
        } else {
            return createClient(configuration.getUrl(), createPermissiveContext(), configuration.getUser(), configuration.getPassword());
        }
    }

    /**
     * Creates a new OpenSearch client.
     *
     * @param url        The URL of the OpenSearch instance to reach
     * @param sslContext The secure socket protocol implementation
     * @param username   The username to authenticate
     * @param password   The password corresponding to the given username
     * @return The OpenSearch client with the given configuration
     */
    public static OpenSearchClient createClient(String url, SSLContext sslContext, String username, String password) {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(ANY, new UsernamePasswordCredentials(username, password));
        return createClient(RestClient
                .builder(HttpHost.create(url))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder
                                .setDefaultCredentialsProvider(credentialsProvider)
                                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                .setSSLContext(sslContext))
                .build());
    }

    /**
     * Creates a new OpenSearch client.
     *
     * @param url        The URL of the OpenSearch instance to reach
     * @param sslContext The secure socket protocol implementation
     * @param apiKey     The encoded API key to authenticate
     * @return The OpenSearch client with the given configuration
     */
    public static OpenSearchClient createClient(String url, SSLContext sslContext, String apiKey) {
        Header headerApiKey = new BasicHeader("Authorization", "ApiKey " + apiKey);
        return createClient(RestClient
                .builder(HttpHost.create(url))
                .setDefaultHeaders(new Header[]{headerApiKey})
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder
                                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                .setSSLContext(sslContext))
                .build());
    }

    /**
     * Creates a new OpenSearch client using the given REST client
     * and using a customized JSON Mapper with Java 8 Date/Time Module.
     *
     * @param restClient The REST client to use
     * @return The OpenSearch client with the given configuration
     */
    public static OpenSearchClient createClient(RestClient restClient) {
        JacksonJsonpMapper jsonMapper = new JacksonJsonpMapper();
        jsonMapper.objectMapper().registerModule(new JavaTimeModule());
        return new OpenSearchClient(new RestClientTransport(restClient, jsonMapper));
    }

    /**
     * Creates a permissive SSL context trusting everything.
     *
     * @return The SSL context
     */
    @SneakyThrows
    public static SSLContext createPermissiveContext() {
        return new SSLContextBuilder()
                .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                .build();
    }

}
