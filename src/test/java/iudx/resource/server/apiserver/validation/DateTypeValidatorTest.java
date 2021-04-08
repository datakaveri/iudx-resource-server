package iudx.resource.server.apiserver.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.validation.types.DateTypeValidator;

@ExtendWith(VertxExtension.class)
public class DateTypeValidatorTest {

  private ParameterTypeValidator dateTypeValidator;
  
  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    dateTypeValidator = new DateTypeValidator().create();
    testContext.completeNow();
  }
  
  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("2020-10-18T14:20:00Z"),
        Arguments.of("2020-10-18T20:45:00+05:30"));
  }
  
  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("date type parameter allowed values.")
  public void testValidDateTypeValue(String value, Vertx vertx, VertxTestContext testContext) {
    RequestParameter result = dateTypeValidator.isValid(value);
    assertEquals(value, result.getString());
    testContext.completeNow();
  }
  
  
  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("","Empty value not allowed for parameter."),
        Arguments.of("  ","Empty value not allowed for parameter."),
        Arguments.of("2020-13-18T14:20:00Z", "Invalid Date format."),
        Arguments.of("2020-10-18V14:20:00Z", "Invalid Date format."),
        Arguments.of("2020-10-18T25:20:00Z", "Invalid Date format."),
        Arguments.of("2020-10-32T14:20:00Z", "Invalid Date format."),
        Arguments.of("date-time", "Invalid Date format."),
        Arguments.of("{{asdbbjas}}", "Invalid Date format."));
  }
  
  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("date type parameter invalid values.")
  public void testInvalidDateTypeValue(String value, String result, Vertx vertx,
      VertxTestContext testContext) {
    ValidationException ex = assertThrows(ValidationException.class, () -> {
      dateTypeValidator.isValid(value);
    });
    assertEquals(result, ex.getMessage());
    testContext.completeNow();
  }
}
