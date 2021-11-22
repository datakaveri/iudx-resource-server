package iudx.resource.server.apiserver.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.apiserver.validation.types.DateTypeValidator;

@ExtendWith(VertxExtension.class)
public class DateTypeValidatorTest {

  private DateTypeValidator dateTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("2020-10-18T14:20:00Z", true),
        Arguments.of("2020-10-18T20:45:00+05:30", true),
        Arguments.of(null, false));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("date type parameter allowed values.")
  public void testValidDateTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    dateTypeValidator = new DateTypeValidator(value, required);
    assertTrue(dateTypeValidator.isValid());
    testContext.completeNow();
  }


  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("", true),
        Arguments.of("  ", true),
        Arguments.of("2020-13-18T14:20:00Z", true),
        Arguments.of("2020-10-18V14:20:00Z", true),
        Arguments.of("2020-10-18T25:20:00Z", true),
        Arguments.of("2020-10-32T14:20:00Z", true),
        Arguments.of("date-time", true),
        Arguments.of("{{asdbbjas}}", true));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("date type parameter invalid values.")
  public void testInvalidDateTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    dateTypeValidator = new DateTypeValidator(value, required);
    assertThrows(DxRuntimeException.class, () -> dateTypeValidator.isValid());
    testContext.completeNow();
  }
}
