package com.chavaillaz.appender.log4j.opensearch;

import static com.chavaillaz.appender.log4j.opensearch.OpensearchUtils.createClient;
import static java.time.OffsetDateTime.now;

import java.util.List;
import java.util.Map;

import com.chavaillaz.appender.log4j.AbstractBatchLogDelivery;
import lombok.extern.log4j.Log4j2;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;

/**
 * Implementation of logs transmission for OpenSearch.
 */
@Log4j2
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
     * @param client        The OpenSearch client to use
     */
    public OpensearchLogDelivery(OpensearchConfiguration configuration, OpenSearchClient client) {
        super(configuration);
        this.client = client;
    }

    @Override
    protected boolean sendBulk(List<Map<String, Object>> documents) {
        try {
            BulkRequest.Builder builder = new BulkRequest.Builder();
            for (Map<String, Object> document : documents) {
                builder.operations(operation -> operation
                        .index(index -> index
                                .index(getConfiguration().generateIndexName(now()))
                                .document(document)));
            }

            BulkResponse response = client.bulk(builder.build());
            if (!response.errors()) {
                log.debug("Bulk of {} documents sent successfully in {}ms", documents.size(), response.took());
                return true;
            }
        } catch (Exception e) {
            log.warn("Unable to send bulk of {} documents: {}", documents.size(), e.getMessage(), e);
        }
        return false;
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (client != null) {
            client._transport().close();
        }
    }

}
