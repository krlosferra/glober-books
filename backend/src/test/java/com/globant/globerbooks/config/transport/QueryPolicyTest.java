package com.globant.globerbooks.config.transport;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.config.transport.QueryPolicy.Outcome;
import com.globant.globerbooks.config.transport.QueryPolicy.Verdict;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests of the query grammar and the id rule (spec BR-011, BR-012, 5.0; E-19, E-26, E-36, E-38, T-14, T-20). */
class QueryPolicyTest {

    private static Verdict list(String query) {
        return QueryPolicy.check(query, true);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void acceptsNoQueryOnAnyEndpoint(String query) {
        assertThat(list(query).outcome()).isEqualTo(Outcome.OK);
        assertThat(QueryPolicy.check(query, false).outcome()).isEqualTo(Outcome.OK);
    }

    @ParameterizedTest
    @ValueSource(strings = {"x=1", "status=reading", "a"})
    void rejectsAnyQueryOffTheList(String query) {
        assertThat(QueryPolicy.check(query, false).outcome()).isEqualTo(Outcome.MALFORMED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unread", "reading", "completed", "%72eading"})
    void acceptsAndDecodesTheThreeStatuses(String value) {
        Verdict verdict = list("status=" + value);
        assertThat(verdict.outcome()).isEqualTo(Outcome.OK);
        assertThat(verdict.status()).contains(value.replace("%72", "r"));
    }

    @Test
    void acceptsEncodedParameterName() {
        assertThat(list("%73tatus=reading")).isEqualTo(new Verdict(Outcome.OK, Optional.of("reading")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"&", "status=reading&", "&status=reading", "foo=1", "Status=reading", "status[]=reading",
            "=reading", "status=a&status=b", "status=reading&status=reading", "status=reading&foo=1",
            "status=%zz", "status=%7", "status=%", "status=%ff", "status=%c3", "%ff=1", "status=Ā",
            "status=%٣٣"})
    void rejectsMalformedShape(String query) {
        assertThat(list(query).outcome()).isEqualTo(Outcome.MALFORMED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"status=READING", "status=", "status", "status=reading+", "status=reading;x=1",
            "status=%20reading", "status=unknown", "status=Completed"})
    void rejectsWrongStatusValueAfterShape(String query) {
        assertThat(list(query).outcome()).isEqualTo(Outcome.INVALID_STATUS);
    }

    @Test
    void shapeErrorWinsOverValueError() {
        assertThat(list("status=BAD&foo=1").outcome()).isEqualTo(Outcome.MALFORMED);
        assertThat(list("status=BAD&status=reading").outcome()).isEqualTo(Outcome.MALFORMED);
    }

    @Test
    void rejectsTwoThousandPairs() {
        String many = "a=b&".repeat(1_999) + "a=b";
        assertThat(list(many).outcome()).isEqualTo(Outcome.MALFORMED);
        assertThat(list("a=b&".repeat(2_000)).outcome()).isEqualTo(Outcome.MALFORMED);
    }

    @Test
    void acceptsLowerCaseUuidOnly() {
        assertThat(QueryPolicy.idWellFormed("3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b")).isTrue();
        assertThat(QueryPolicy.idWellFormed("00000000-0000-0000-0000-000000000000")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"3F2B8C1E-5A4D-4E6F-9B7A-1C2D3E4F5A6B", "not-a-uuid", "",
            "1%27%20OR%20%271%27%3D%271", "%33f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b",
            "3f2b8c1e5a4d4e6f9b7a1c2d3e4f5a6b", "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6",
            "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6bb", "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6g",
            "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6B", " 3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6"})
    void rejectsMalformedIds(String id) {
        assertThat(QueryPolicy.idWellFormed(id)).isFalse();
    }

    @Test
    void rejectsNullId() {
        assertThat(QueryPolicy.idWellFormed(null)).isFalse();
    }
}
