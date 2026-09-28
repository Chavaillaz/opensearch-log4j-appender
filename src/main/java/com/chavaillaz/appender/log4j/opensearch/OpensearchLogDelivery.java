package com.chavaillaz.appender.log4j.opensearch;

import static com.chavaillaz.appender.log4j.opensearch.OpensearchUtils.createClient;
import static java.time.OffsetDateTime.now;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.chavaillaz.appender.TransmissionResult;
import com.chavaillaz.appender.log4j.AbstractBatchLogDelivery;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;

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

            return completedFuture(toResult(documents, client.bulk(builder.build())));
        } catch (Exception e) {
            // Logged with its cause and retried by the library
            return completedFuture(TransmissionResult.failure(e));
        }
    }

    /**
     * Turns the response to a bulk request into the outcome of its transmission: only the documents
     * OpenSearch rejected, for a mapping conflict or a full queue for instance, are retried, the others
     * being indexed already, and all of them are when the response does not tell them apart.
     *
     * @param documents The documents sent, in the order of the request
     * @param response  The response to the request
     * @return The outcome of the transmission
     */
    static TransmissionResult<Map<String, Object>> toResult(List<Map<String, Object>> documents, BulkResponse response) {
        if (!response.errors()) {
            return TransmissionResult.success();
        }
        List<BulkResponseItem> items = response.items();
        if (items.size() != documents.size()) {
            return TransmissionResult.failure(new IllegalStateException(
                    "OpenSearch answered " + items.size() + " items to a bulk of " + documents.size() + " documents"));
        }

        List<Map<String, Object>> rejected = new ArrayList<>();
        BulkResponseItem firstRejection = null;
        for (int i = 0; i < items.size(); i++) {
            BulkResponseItem item = items.get(i);
            if (item.error() != null) {
                rejected.add(documents.get(i));
                if (firstRejection == null) {
                    firstRejection = item;
                }
            }
        }
        if (firstRejection == null) {
            return TransmissionResult.failure(new IllegalStateException(
                    "OpenSearch reported errors for a bulk without naming the documents in error"));
        }

        BulkResponseItem rejection = firstRejection;
        OpenSearchException cause = new OpenSearchException(ErrorResponse.of(error -> error
                .error(rejection.error())
                .status(rejection.status())));
        return rejected.size() == documents.size()
                ? TransmissionResult.failure(cause)
                : TransmissionResult.partialFailure(rejected, cause);
    }

    @Override
    public void close(@Nullable Duration timeout) throws Exception {
        super.close(timeout);
        client._transport().close();
    }

}
