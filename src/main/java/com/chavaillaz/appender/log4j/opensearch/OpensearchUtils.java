package com.chavaillaz.appender.log4j.opensearch;

import java.security.GeneralSecurityException;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.opensearch.client.json.jackson3.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

/**
 * OpenSearch specific utility methods.
 */
@UtilityClass
public class OpensearchUtils {

    /**
     * Creates a new OpenSearch client, authenticating with the API key, or the user when there is none, or
     * not at all for a cluster without security.
     *
     * @param configuration The configuration to use
     * @return The OpenSearch client with the given configuration
     */
    public static OpenSearchClient createClient(OpensearchConfiguration configuration) {
        String apiKey = configuration.getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            return createClient(configuration.getUrl(), configuration.isUrlTrusted(), apiKey);
        } else if (configuration.getUser() != null) {
            return createClient(configuration.getUrl(), configuration.isUrlTrusted(), configuration.getUser(), configuration.getPassword());
        } else {
            return createClient(configuration.getUrl(), configuration.isUrlTrusted());
        }
    }

    /**
     * Creates a new OpenSearch client without authentication, for a cluster without security.
     *
     * @param url     The URL of the OpenSearch instance to reach
     * @param trusted If {@code true} the client will trust all certificates
     * @return The OpenSearch client with the given configuration
     */
    @SneakyThrows
    public static OpenSearchClient createClient(String url, boolean trusted) {
        AsyncClientConnectionManager connectionManager = getClientConnectionManager(trusted);
        return new OpenSearchClient(ApacheHttpClient5TransportBuilder
                .builder(HttpHost.create(url))
                .setMapper(getJsonMapper())
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setConnectionManager(connectionManager))
                .build());
    }

    /**
     * Creates a new OpenSearch client.
     *
     * @param url      The URL of the OpenSearch instance to reach
     * @param trusted  If {@code true} the client will trust all certificates
     * @param username The username to authenticate
     * @param password The password corresponding to the given username, or {@code null} for none
     * @return The OpenSearch client with the given configuration
     */
    @SneakyThrows
    public static OpenSearchClient createClient(String url, boolean trusted, String username, String password) {
        AsyncClientConnectionManager connectionManager = getClientConnectionManager(trusted);
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(null, -1),
                new UsernamePasswordCredentials(username, password != null ? password.toCharArray() : new char[0])
        );

        return new OpenSearchClient(ApacheHttpClient5TransportBuilder
                .builder(HttpHost.create(url))
                .setMapper(getJsonMapper())
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .setConnectionManager(connectionManager))
                .build());
    }

    /**
     * Creates a new OpenSearch client.
     *
     * @param url     The URL of the OpenSearch instance to reach
     * @param trusted If {@code true} the client will trust all certificates
     * @param apiKey  The encoded API key to authenticate
     * @return The OpenSearch client with the given configuration
     */
    @SneakyThrows
    public static OpenSearchClient createClient(String url, boolean trusted, String apiKey) {
        AsyncClientConnectionManager connectionManager = getClientConnectionManager(trusted);
        Header headerApiKey = new BasicHeader("Authorization", "ApiKey " + apiKey);

        return new OpenSearchClient(ApacheHttpClient5TransportBuilder
                .builder(HttpHost.create(url))
                .setMapper(getJsonMapper())
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setConnectionManager(connectionManager))
                .setDefaultHeaders(new Header[]{headerApiKey})
                .build());
    }

    /**
     * Creates a new JSON mapper using Jackson.
     *
     * @return The mapper with Java time module registered
     */
    public static JacksonJsonpMapper getJsonMapper() {
        return new JacksonJsonpMapper();
    }

    /**
     * Creates a new client connection manager.
     *
     * @param trustAll If {@code true} the connection manager will trust all certificates
     * @return The connection manager with a custom TLS strategy
     * @throws GeneralSecurityException If there is an issue creating the SSL context
     */
    public static AsyncClientConnectionManager getClientConnectionManager(boolean trustAll) throws GeneralSecurityException {
        TlsStrategy trustAllStrategy = new DefaultClientTlsStrategy(SSLContexts.custom()
                .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                .build(), NoopHostnameVerifier.INSTANCE);
        return PoolingAsyncClientConnectionManagerBuilder.create()
                .setTlsStrategy(trustAll ? trustAllStrategy : null)
                .build();
    }

}
