package com.globant.globerbooks;

import static com.globant.globerbooks.support.UnicodeFixtures.ACUTE;
import static com.globant.globerbooks.support.UnicodeFixtures.HANGUL_FILLER;
import static com.globant.globerbooks.support.UnicodeFixtures.VS16;
import static com.globant.globerbooks.support.UnicodeFixtures.ZWJ;
import static com.globant.globerbooks.support.UnicodeFixtures.ZWNJ;
import static com.globant.globerbooks.support.UnicodeFixtures.of;
import static com.globant.globerbooks.support.UnicodeFixtures.repeat;
import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.support.ApiCalls;
import com.globant.globerbooks.support.ApiCalls.Reply;
import com.globant.globerbooks.support.AppProcess;
import com.globant.globerbooks.support.FreshApp;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/**
 * The hidden-channel text rules of BR-006 repeated on the real port against the packaged jar (Task 49+50,
 * criterion 6; spec Appendix D T-23, T-24, T-24b, T-25, T-26). Every row is built from numeric code points. A
 * row the spec accepts answers 201; a row it refuses answers 400 {@code VALIDATION_ERROR} with the field
 * {@code title}.
 */
class TextAndParserIntegrationTest {

    private static final int VS_SUPPLEMENT_1 = 0xE0100;
    private static final int VS_SUPPLEMENT_2 = 0xE0101;

    @TempDir
    static Path temp;

    private static AppProcess app;
    private static ApiCalls api;
    private static int counter;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeAll
    static void start() throws Exception {
        app = FreshApp.start(temp);
        api = app.api();
    }

    @AfterAll
    static void stop() throws Exception {
        FreshApp.stopAndCheck(app);
    }

    private Reply post(String title) throws Exception {
        counter++;
        Map<String, String> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("author", "Text author " + counter);
        return api.call("POST", "/api/v1/books", json.writeValueAsString(body));
    }

    private void accepted(String label, String title) throws Exception {
        Reply reply = post(title);
        System.out.println("TEXT " + label + " | " + reply.label());
        assertThat(reply.status()).as(label).isEqualTo(201);
    }

    private void refused(String label, String title) throws Exception {
        Reply reply = post(title);
        System.out.println("TEXT " + label + " | " + reply.label());
        assertThat(reply.status()).as(label).isEqualTo(400);
        assertThat(reply.code()).as(label).isEqualTo("VALIDATION_ERROR");
        assertThat(reply.body()).as(label).contains("\"field\":\"title\"");
    }

    @Test
    void t23BlankLookingTitlesAreRefusedAndAFillerBetweenVisibleCharactersIsAccepted() throws Exception {
        refused("T-23 U+3164 only", of(HANGUL_FILLER));
        refused("T-23 U+2800 only", of(0x2800));
        refused("T-23 U+0301 only", of(ACUTE));
        refused("T-23 U+0301 then abc", of(ACUTE) + "abc");
        refused("T-23 U+FE0F only", of(VS16));
        accepted("T-23 ab U+3164 cd", "ab" + of(HANGUL_FILLER) + "cd");
    }

    @Test
    void t24RunsOfCombiningMarksAreBoundedAtEight() throws Exception {
        refused("T-24 x + 9 marks", "x" + repeat(ACUTE, 9));
        accepted("T-24 x + 8 marks", "x" + repeat(ACUTE, 8));
        refused("T-24 a + 2 variation selectors", "a" + of(VS_SUPPLEMENT_1, VS_SUPPLEMENT_2));
        accepted("T-24 a + 1 variation selector", "a" + of(VS_SUPPLEMENT_1));
    }

    @Test
    void t24bJoinersDoNotHideARunOfMarks() throws Exception {
        String burst = repeat(ACUTE, 8) + of(ZWJ);
        refused("T-24b run of 10", "x" + repeat(ACUTE, 8) + of(ZWJ, ACUTE));
        refused("T-24b 50 bursts", "x" + burst.repeat(50) + "y");
        refused("T-24b VS16 ZWJ VS16", "a" + of(VS16, ZWJ, VS16));
        refused("T-24b mark VS16 mark VS16", "a" + of(ACUTE, VS16, ACUTE, VS16));
        accepted("T-24b run of 8", "x" + repeat(ACUTE, 7) + of(ZWJ) + "y");
        accepted("T-24b heart fire", of(0x2764, VS16, ZWJ, 0x1F525));
    }

    @Test
    void t25JoinersAreAllowedOnlyBetweenLettersInTheAllowedShapes() throws Exception {
        accepted("T-25 Persian ZWNJ", "می" + of(ZWNJ) + "خواهم");
        refused("T-25 ZWNJ first", of(ZWNJ) + "abc");
        refused("T-25 ZWNJ last", "abc" + of(ZWNJ));
        refused("T-25 ZWNJ then ZWJ", "a" + of(ZWNJ, ZWJ) + "b");
        refused("T-25 ZWJ then ZWJ", "a" + of(ZWJ, ZWJ) + "b");
        refused("T-25 joiner next to a space", "a " + of(ZWJ) + "b");
    }

    @Test
    void t26ReplacementCharacterIsAcceptedAndNoncharactersAndUnassignedAreRefused() throws Exception {
        accepted("T-26 U+FFFD between letters", "ab" + of(0xFFFD) + "cd");
        refused("T-26 U+FFFE", of(0xFFFE));
        refused("T-26 U+FDD0", of(0xFDD0));
        refused("T-26 U+0378", of(0x0378));
    }
}
