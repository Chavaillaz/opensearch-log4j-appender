package com.chavaillaz.appender.log4j.opensearch;

import static com.chavaillaz.appender.log4j.opensearch.OpensearchUtils.createClient;
import static java.time.OffsetDateTime.now;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.chavaillaz.appender.TransmissionResult;
import com.chavaillaz.appender.log4j.AbstractBatchLogDelivery;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;

/**
 * Implementation of logs transmission for OpenSearch.
 */
public class OpensearchLogDelivery extends AbstractBatchLogDelivery<OpensearchConfiguration> {

    private final OpenSearchClient client;

    /**
     * Creates a new logs delivery handler for OpenSearch.
     *
     * @param configuration The configuration to use
     */
    public OpensearchLogDelivery(OpensearchConfiguration configuration) {
        this(configuration, createClient(configuration));
    }

    /**
     * Creates a new logs delivery handler for OpenSearch.
     *
     * @param configuration The configuration to use
     * @param client        The OpenSearch client to use, whose transport is closed along with this delivery
     */
    public OpensearchLogDelivery(OpensearchConfiguration configuration, OpenSearchClient client) {
        super(configuration);
        this.client = client;
    }

    @Override
    protected CompletableFuture<TransmissionResult<Map<String, Object>>> sendBulk(List<Map<String, Object>> documents) {
        try {
            String index = getConfiguration().generateIndexName(now());
            BulkRequest.Builder builder = new BulkRequest.Builder();
            for (Map<String, Object> document : documents) {
                builder.operations(operation -> operation
                        .index(action -> action
                                .index(index)
                                .document(document)));
            }

            BulkResponse response = client.bulk(builder.build());
            return completedFuture(response.errors() ? TransmissionResult.failure(null) : TransmissionResult.success());
        } catch (Exception e) {
            // Logged with its cause and retried by the library
            return completedFuture(TransmissionResult.failure(e));
        }
    }

    @Override
    public void close(@Nullable Duration timeout) throws Exception {
        super.close(timeout);
        client._transport().close();
    }

}
