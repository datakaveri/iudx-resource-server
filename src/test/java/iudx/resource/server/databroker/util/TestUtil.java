package iudx.resource.server.databroker.util;

import io.vertx.core.json.JsonArray;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestUtil {

    @Test
    @DisplayName("Test getSha method : with Exception")
    public void test_getSha_with_NULL_input(VertxTestContext vertxTestContext) {
        String actual = Util.getSha(null);
        System.out.println(actual);
        assertNull(actual);
        vertxTestContext.completeNow();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Dummy_username", ""})
    @DisplayName("Test getSha method ")
    public void test_getSha(String plainUserName, VertxTestContext vertxTestContext) {
        String actual = Util.getSha(plainUserName);
        assertNotNull(actual);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test generateRandomPassword method")
    public void test_generateRandomPassword(VertxTestContext vertxTestContext) {
        String actual = Util.generateRandomPassword();
        assertNotNull(actual);
        vertxTestContext.completeNow();
    }

    @ParameterizedTest
    @ValueSource(strings = {"&&&", "---+", ":-)", ":-|", ":-("})
    @DisplayName("Test isValidID method")
    public void test_isValidID_with_invalid_input(String id, VertxTestContext vertxTestContext) {
        boolean actual = Util.isValidId.test(id);
        assertFalse(actual);
        assertFalse(Util.isValidId.test(null));
        vertxTestContext.completeNow();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd", "12445", "3aervdsfg", ""})
    @DisplayName("Test isValidID method")
    public void test_isValidID_with_valid_input(String id, VertxTestContext vertxTestContext) {
        boolean actual = Util.isValidId.test(id);
        assertTrue(actual);
        vertxTestContext.completeNow();
    }


    @Test
    @DisplayName("Test bindingMergeOperator  method")
    public void test_bindingMergeOperator(VertxTestContext vertxTestContext) {
        JsonArray t = new JsonArray();
        t.add(0, "Dummy key1");
        JsonArray u = new JsonArray();
        u.add(0, "Dummy key2");
        JsonArray expected = new JsonArray();
        expected.add(0, "Dummy key2");
        expected.add(0, "Dummy key1");
        assertEquals(expected, Util.bindingMergeOperator.apply(t, u));
        vertxTestContext.completeNow();
    }

}
