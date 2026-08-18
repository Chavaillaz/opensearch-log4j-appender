package com.chavaillaz.appender.log4j.opensearch;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.generic.Body;
import org.opensearch.client.opensearch.generic.Requests;
import org.opensearch.client.opensearch.generic.Response;

public class OpensearchApiKeyHelper {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String REQUEST = """
            {
                "name": "my-test-key",
                "cluster_permissions": ["cluster:*"],
                "index_permissions": [
                  {
                    "index_pattern": ["*"],
                    "allowed_actions": ["indices:*"]
                  }
                ],
                "duration_seconds": 2592000
              }
            """;

    /**
     * Creates a Security plugin API key and returns the generated key string.
     *
     * @param client an initialized OpenSearchClient
     * @return the generated API key value
     */
    public static String createApiKey(OpenSearchClient client) throws IOException {
        try (Response response = client.generic().execute(
                Requests.builder()
                        .endpoint("/_plugins/_security/api/apitokens")
                        .method("POST")
                        .json(REQUEST)
                        .build())) {

            String body = response.getBody()
                    .map(Body::bodyAsString)
                    .orElse("");

            if (response.getStatus() >= 300) {
                throw new IOException("Failed to create API key (status " + response.getStatus() + "): " + body);
            }

            return extractApiKey(body);
        }
    }

    private static String extractApiKey(String responseBody) throws IOException {
        JsonNode root = MAPPER.readTree(responseBody);

        String[] candidateFields = {"api_key", "apiKey", "token", "key"};
        for (String field : candidateFields) {
            if (root.hasNonNull(field)) {
                return root.get(field).asText();
            }
        }

        throw new IOException("Could not find API key field in response. Raw response: " + responseBody);
    }

}
