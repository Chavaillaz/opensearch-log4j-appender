package com.chavaillaz.appender.log4j.opensearch;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import com.chavaillaz.appender.log4j.DefaultLogConverter;
import com.chavaillaz.appender.log4j.LogConverter;
import org.junit.jupiter.api.Test;

class OpensearchConfigurationTest {

    @Test
    void testGenerateIndexNameDate() {
        // Given
        String index = "idx";
        String suffix = "yyyy.MM.dd";
        OpensearchConfiguration configuration = new OpensearchConfiguration();
        configuration.setIndex(index);
        configuration.setIndexSuffix("-" + suffix);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        OffsetDateTime date = now();

        // When
        String indexName = configuration.generateIndexName(date.toString());

        // Then
        assertThat(indexName).isEqualTo(index + "-" + formatter.format(date));
    }

    @Test
    void testWrongConverter() {
        // Given
        OpensearchConfiguration configuration = new OpensearchConfiguration();
        configuration.setConverter("package.does.not.exist.NotFoundConverter");

        // When
        LogConverter converter = configuration.getConverter();

        // Then
        assertThat(converter.getClass()).isEqualTo(DefaultLogConverter.class);
    }

}
