package com.chavaillaz.appender.log4j.opensearch;

import static com.chavaillaz.appender.log4j.opensearch.OpensearchUtils.createClient;
import static com.chavaillaz.appender.log4j.opensearch.OpensearchUtils.createPermissiveContext;
import static java.time.OffsetDateTime.now;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
        super(configuration);

        if (isNotBlank(configuration.getApiKey())) {
            client = createClient(configuration.getUrl(), createPermissiveContext(), configuration.getApiKey());
        } else {
            client = createClient(configuration.getUrl(), createPermissiveContext(), configuration.getUser(), configuration.getPassword());
        }
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
                log.debug("Bulk of {} elements sent successfully in {}ms", documents.size(), response.took());
                return true;
            }
        } catch (Exception e) {
            log.warn("Error when sending bulk", e);
        }
        return false;
    }

    @Override
    public void close() {
        if (client != null) {
            super.close();
        }
    }

}
