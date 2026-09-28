package com.chavaillaz.appender.log4j.opensearch;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.chavaillaz.appender.DeliveryOptions;
import com.chavaillaz.appender.LogConfiguration;
import com.chavaillaz.appender.log4j.DefaultLogConverter;
import com.chavaillaz.appender.log4j.LogConverter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.junit.jupiter.api.Test;

class OpensearchConfigurationTest {

    @Test
    void testGenerateIndexNameDate() {
        // Given
        String index = "idx";
        String suffix = "yyyy.MM.dd";
        OpensearchConfiguration configuration = OpensearchConfiguration.builder()
                .index(index)
                .indexSuffix(DateTimeFormatter.ofPattern("-" + suffix))
                .build();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(suffix);
        OffsetDateTime date = now();

        // When
        String indexName = configuration.generateIndexName(date.toString());

        // Then
        assertThat(indexName).isEqualTo(index + "-" + formatter.format(date));
    }

    @Test
    void testGenerateIndexNameWithoutSuffix() {
        // Given
        OpensearchConfiguration configuration = OpensearchConfiguration.builder()
                .index("idx")
                .build();

        // When
        String indexName = configuration.generateIndexName(now());

        // Then
        assertThat(indexName).isEqualTo("idx");
    }

    @Test
    void testDeliveryOptionsFromAttributes() {
        // Given
        OpensearchAppender.Builder builder = OpensearchAppender.builder()
                .setName("appender")
                .setFlushLevel("warn")
                .setMaxRequestBytes(5_242_880)
                .setRetryBackoff(0)
                .setCloseTimeout(5_000);

        // When
        DeliveryOptions options = builder.build().getLogConfiguration().getDeliveryOptions();

        // Then: the attributes set are applied, the others keeping their defaults
        assertThat(options.flushLevel()).isEqualTo(Level.WARN);
        assertThat(options.maxRequestBytes()).isEqualTo(5_242_880);
        assertThat(options.retryBackoff()).isZero();
        assertThat(options.closeTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(options.flushThreshold()).isEqualTo(100);
        assertThat(options.maxPendingDocuments()).isEqualTo(DeliveryOptions.defaults().maxPendingDocuments());
    }

    @Test
    void testDefaultsSplitRequestsPastTenMebibytes() {
        // When: neither attributes nor options given
        DeliveryOptions fromAttributes = OpensearchAppender.builder()
                .setName("appender")
                .build()
                .getLogConfiguration()
                .getDeliveryOptions();
        DeliveryOptions fromConfiguration = OpensearchConfiguration.builder()
                .build()
                .getDeliveryOptions();

        // Then: the same defaults either way, a bulk request never growing past 10 MiB
        assertThat(fromAttributes).isEqualTo(OpensearchConfiguration.DEFAULT_DELIVERY_OPTIONS);
        assertThat(fromConfiguration).isEqualTo(OpensearchConfiguration.DEFAULT_DELIVERY_OPTIONS);
        assertThat(fromAttributes.maxRequestBytes()).isEqualTo(10_485_760);
        assertThat(fromAttributes.flushThreshold()).isEqualTo(100);
    }

    @Test
    void testHostNameSetIsKept() {
        // Given
        OpensearchAppender.Builder builder = OpensearchAppender.builder()
                .setName("appender")
                .setHostName("my-host");

        // When
        String host = builder.build().getLogConfiguration().getHost();

        // Then
        assertThat(host).isEqualTo("my-host");
    }

    @Test
    void testHostNameIsOnlyResolvedWhenBuilding() {
        // Given: a builder, which Log4j2 creates before setting the attributes of the configuration
        OpensearchAppender.Builder builder = OpensearchAppender.builder()
                .setName("appender");

        // When / Then: nothing looked up yet, however long the lookup takes, the host possibly being given later
        assertThat(builder.getHostName()).isNull();
        assertThat(builder.build().getLogConfiguration().getHost()).isNotBlank();
    }

    @Test
    void testUnknownFlushLevelFailsTheConfiguration() {
        // Given
        OpensearchAppender.Builder builder = OpensearchAppender.builder()
                .setName("appender")
                .setFlushLevel("WARNING");

        // When / Then: a misspelled level fails the configuration rather than disabling the flush silently
        assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testWrongConverter() {
        // Given
        OpensearchAppender appender = OpensearchAppender.builder()
                .setName("appender")
                .setConverter("package.does.not.exist.NotFoundConverter")
                .build();

        // When
        LogConverter converter = appender.getLogConverter();

        // Then
        assertThat(converter.getClass()).isEqualTo(DefaultLogConverter.class);
    }

    @Test
    void testNamedConverterIsBuiltWithTheConfiguration() {
        // Given
        OpensearchAppender appender = OpensearchAppender.builder()
                .setName("appender")
                .setApplicationName("my-application")
                .setConverter(ApplicationConverter.class.getName())
                .build();

        // When
        LogConverter converter = appender.getLogConverter();

        // Then
        assertThat(converter).isInstanceOf(ApplicationConverter.class);
        assertThat(converter.convert(Log4jLogEvent.newBuilder().build())).containsEntry("application", "my-application");
    }

    public static class ApplicationConverter implements LogConverter {

        private final LogConfiguration configuration;

        public ApplicationConverter(LogConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public Map<String, Object> convert(LogEvent event) {
            return Map.of("application", configuration.getApplication());
        }

    }

}
