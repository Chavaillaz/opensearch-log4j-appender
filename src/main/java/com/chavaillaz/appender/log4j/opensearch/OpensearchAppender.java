package com.chavaillaz.appender.log4j.opensearch;

import static com.chavaillaz.appender.CommonUtils.getInitialHostname;
import static com.chavaillaz.appender.CommonUtils.getProperty;
import static org.apache.logging.log4j.core.Appender.ELEMENT_TYPE;
import static org.apache.logging.log4j.core.Core.CATEGORY_NAME;
import static org.apache.logging.log4j.core.layout.PatternLayout.createDefaultLayout;

import java.time.Duration;
import java.util.Optional;

import com.chavaillaz.appender.LogDelivery;
import com.chavaillaz.appender.log4j.AbstractLogDeliveryAppender;
import com.chavaillaz.appender.log4j.DefaultLogConverter;
import com.chavaillaz.appender.log4j.LogConverter;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

/**
 * Appender implementation using OpenSearch for transmissions of logs from Log4j.
 */
@Plugin(name = "OpensearchAppender", category = CATEGORY_NAME, elementType = ELEMENT_TYPE)
public class OpensearchAppender extends AbstractLogDeliveryAppender<OpensearchConfiguration> {

    protected OpensearchAppender(String name, Filter filter, Layout<?> layout, OpensearchConfiguration configuration) {
        super(name, filter, layout, configuration);
    }

    @PluginBuilderFactory
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LogDelivery createLogDeliveryHandler() {
        return new OpensearchLogDelivery(getLogConfiguration());
    }

    @Override
    public Runnable createLogDeliveryTask(LogEvent loggingEvent) {
        LogConverter converter = getLogConfiguration().getConverter();
        LogEvent immutableEvent = loggingEvent.toImmutable();
        return () -> Optional.ofNullable(getLogDeliveryHandler())
                .ifPresent(handler -> handler.send(converter.convert(immutableEvent)));
    }

    @Setter
    @Getter
    @Accessors(chain = true)
    public static class Builder implements org.apache.logging.log4j.core.util.Builder<OpensearchAppender> {

        @PluginBuilderAttribute
        @Required(message = "No appender name provided")
        private String name;

        @PluginElement("Layout")
        private Layout<String> layout = createDefaultLayout();

        @PluginElement("Filter")
        private Filter filter;

        @PluginBuilderAttribute("Application")
        private String applicationName = getProperty("APP", "unknown");

        @PluginBuilderAttribute("Host")
        private String hostName = getProperty("HOST", getInitialHostname());

        @PluginBuilderAttribute("Environment")
        private String environmentName = getProperty("ENV", "local");

        @PluginBuilderAttribute("Converter")
        private String converter = getProperty("CONVERTER", DefaultLogConverter.class.getName());

        @PluginBuilderAttribute("Index")
        private String index = getProperty("INDEX", "ha");

        @PluginBuilderAttribute("IndexSuffix")
        private String indexSuffix = getProperty("INDEX_SUFFIX", "");

        @PluginBuilderAttribute("Url")
        private String url = getProperty("OPENSEARCH_URL", null);

        @PluginBuilderAttribute("UrlTrusted")
        private boolean urlTrusted = false;

        @PluginBuilderAttribute("User")
        private String user = getProperty("OPENSEARCH_USER", null);

        @PluginBuilderAttribute("Password")
        private String password = getProperty("OPENSEARCH_PASSWORD", null);

        @PluginBuilderAttribute("ApiKey")
        private String apiKey = getProperty("OPENSEARCH_API_KEY", null);

        @PluginBuilderAttribute("FlushThreshold")
        private long flushThreshold = 100;

        @PluginBuilderAttribute("FlushInterval")
        private long flushInterval = 5_000;

        @Override
        public OpensearchAppender build() {
            OpensearchConfiguration configuration = new OpensearchConfiguration();
            configuration.setApplication(getApplicationName());
            configuration.setHost(getHostName());
            configuration.setEnvironment(getEnvironmentName());
            configuration.setConverter(getConverter());
            configuration.setIndex(getIndex());
            configuration.setIndexSuffix(getIndexSuffix());
            configuration.setUrl(getUrl());
            configuration.setUrlTrusted(isUrlTrusted());
            configuration.setUser(getUser());
            configuration.setPassword(getPassword());
            configuration.setApiKey(getApiKey());
            configuration.setFlushThreshold(getFlushThreshold());
            configuration.setFlushInterval(Duration.ofMillis(getFlushInterval()));
            return new OpensearchAppender(getName(), getFilter(), getLayout(), configuration);
        }

    }

}