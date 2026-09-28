package com.chavaillaz.appender.log4j.opensearch;

import static com.chavaillaz.appender.CommonUtils.getInitialHostname;
import static com.chavaillaz.appender.CommonUtils.getProperty;
import static org.apache.logging.log4j.core.Appender.ELEMENT_TYPE;
import static org.apache.logging.log4j.core.Core.CATEGORY_NAME;
import static org.apache.logging.log4j.core.layout.PatternLayout.createDefaultLayout;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

import com.chavaillaz.appender.DeliveryOptions;
import com.chavaillaz.appender.LogDelivery;
import com.chavaillaz.appender.log4j.AbstractLogDeliveryAppender;
import com.chavaillaz.appender.log4j.DefaultLogConverter;
import com.chavaillaz.appender.log4j.LogConverter;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.jspecify.annotations.Nullable;

/**
 * Appender implementation using OpenSearch for transmissions of logs from Log4j.
 */
@Plugin(name = "OpensearchAppender", category = CATEGORY_NAME, elementType = ELEMENT_TYPE)
public class OpensearchAppender extends AbstractLogDeliveryAppender<OpensearchConfiguration> {

    /**
     * The status logger of Log4j2 rather than an application logger, which would feed these diagnostics back
     * into this appender.
     */
    private static final Logger log = StatusLogger.getLogger();

    /**
     * The converter the configuration names, or {@code null} for the default one of the appender.
     */
    private final @Nullable LogConverter converter;

    protected OpensearchAppender(String name, @Nullable Filter filter, @Nullable Layout<?> layout, OpensearchConfiguration configuration) {
        super(name, filter, layout, configuration);
        this.converter = createConverter(configuration);
    }

    @PluginBuilderFactory
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates the converter the configuration names, configured with it.
     *
     * @param configuration The configuration naming the converter
     * @return The converter, or {@code null} for the default one, which is also used, and reported, when the
     *         converter named cannot be created
     */
    private static @Nullable LogConverter createConverter(OpensearchConfiguration configuration) {
        String className = configuration.getConverter();
        if (className == null || className.isBlank() || DefaultLogConverter.class.getName().equals(className)) {
            return null;
        }
        try {
            LogConverter created = LoaderUtil.newCheckedInstanceOf(className, LogConverter.class);
            created.configure(configuration);
            return created;
        } catch (Exception | LinkageError e) {
            log.error("Unable to create the converter {}, converting with the default one instead", className, e);
            return null;
        }
    }

    @Override
    public LogDelivery createLogDeliveryHandler() {
        return new OpensearchLogDelivery(getLogConfiguration());
    }

    @Override
    protected LogConverter getLogConverter() {
        return converter != null ? converter : super.getLogConverter();
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
            OpensearchConfiguration configuration = OpensearchConfiguration.builder()
                    .application(getApplicationName())
                    .host(getHostName())
                    .environment(getEnvironmentName())
                    .converter(getConverter())
                    .index(getIndex())
                    .indexSuffix(toFormatter(getIndexSuffix()))
                    .url(getUrl())
                    .urlTrusted(isUrlTrusted())
                    .user(getUser())
                    .password(getPassword())
                    .apiKey(getApiKey())
                    .deliveryOptions(DeliveryOptions.builder()
                            .flushThreshold(getFlushThreshold())
                            .flushInterval(Duration.ofMillis(getFlushInterval()))
                            .build())
                    .build();
            return new OpensearchAppender(getName(), getFilter(), getLayout(), configuration);
        }

        /**
         * Parses the pattern of the index suffix.
         *
         * @param pattern The pattern, following {@link DateTimeFormatter}, or an empty one for no suffix
         * @return The formatter, or {@code null} for no suffix
         */
        private static @Nullable DateTimeFormatter toFormatter(@Nullable String pattern) {
            return pattern == null || pattern.isEmpty() ? null : DateTimeFormatter.ofPattern(pattern);
        }

    }

}
