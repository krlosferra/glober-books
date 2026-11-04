package com.globant.globerbooks.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests of the status wire values (US-002.6 values, BR-001). */
class BookStatusTest {

    @Test
    void wireValuesAreTheThreeLowerCaseWords() {
        assertThat(BookStatus.UNREAD.wireValue()).isEqualTo("unread");
        assertThat(BookStatus.READING.wireValue()).isEqualTo("reading");
        assertThat(BookStatus.COMPLETED.wireValue()).isEqualTo("completed");
    }

    @Test
    void fromWireFindsEachExactValue() {
        assertThat(BookStatus.fromWire("unread")).contains(BookStatus.UNREAD);
        assertThat(BookStatus.fromWire("reading")).contains(BookStatus.READING);
        assertThat(BookStatus.fromWire("completed")).contains(BookStatus.COMPLETED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"READING", " reading", "reading ", "", "Completed", "done"})
    void fromWireRejectsAnythingButTheExactValues(String value) {
        assertThat(BookStatus.fromWire(value)).isEmpty();
    }

    @Test
    void fromWireRejectsNull() {
        assertThat(BookStatus.fromWire(null)).isEmpty();
    }

    @Test
    void converterWritesLowerCaseAndReadsItBack() {
        BookStatus.WireConverter converter = new BookStatus.WireConverter();

        assertThat(converter.convertToDatabaseColumn(BookStatus.COMPLETED)).isEqualTo("completed");
        assertThat(converter.convertToEntityAttribute("reading")).isEqualTo(BookStatus.READING);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void converterRefusesAnUnknownStoredValue() {
        BookStatus.WireConverter converter = new BookStatus.WireConverter();

        assertThatThrownBy(() -> converter.convertToEntityAttribute("READING"))
                .isInstanceOf(IllegalStateException.class);
    }
}
