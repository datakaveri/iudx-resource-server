package iudx.resource.server.apiserver.validation;

import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.apiserver.validation.types.StringTypeValidator;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class StringTypeValidatorTest {
  private StringTypeValidator stringTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    // Add any valid value for which validation will pass successfully.
    return Stream.of(
        Arguments.of("within", true, "within"),
        Arguments.of("intersects", true, "intersects"),
        Arguments.of("near", true, "near"),
        Arguments.of(null, false, null));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("georel parameter allowed values.")
  public void testValidGeoRelValue(
      String value,
      boolean required,
      Pattern regexPattern,
      Vertx vertx,
      VertxTestContext testContext) {
    stringTypeValidator = new StringTypeValidator(value, required, regexPattern);
    assertTrue(stringTypeValidator.isValid());
    testContext.completeNow();
  }

  static Stream<Arguments> invalidValues() {
    // Add any invalid value for which validation must fail.
    String random600Id = RandomStringUtils.random(600);
    return Stream.of(
        Arguments.of("", true, ""),
        Arguments.of("  ", true, " "),
        Arguments.of("around", true, "bypaas"),
        Arguments.of("", false, ""),
        Arguments.of(null, true, ""));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("geo parameter invalid values.")
  public void testInvalidGeoRelValue(
      String value,
      boolean required,
      Pattern regexPattern,
      Vertx vertx,
      VertxTestContext testContext) {
    stringTypeValidator = new StringTypeValidator(value, required, regexPattern);
    assertThrows(DxRuntimeException.class, () -> stringTypeValidator.isValid());
    testContext.completeNow();
  }
}
