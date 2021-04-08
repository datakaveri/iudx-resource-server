package iudx.resource.server.apiserver.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.stream.Stream;
import org.apache.commons.lang.math.RandomUtils;
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
import iudx.resource.server.apiserver.validation.types.DistanceTypeValidator;

@ExtendWith(VertxExtension.class)
public class DistanceTypeValidatorTest {

  private ParameterTypeValidator distanceTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    distanceTypeValidator = new DistanceTypeValidator().create();
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("0.1"),
        Arguments.of("500"));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("distance parameter allowed values.")
  public void testValidDistanceValue(String value, Vertx vertx, VertxTestContext testContext) {
    RequestParameter result = distanceTypeValidator.isValid(value);
    assertEquals(value, result.getString());
    testContext.completeNow();
  }
  
  
  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    String random600Id=RandomStringUtils.random(600);
    return Stream.of(
        Arguments.of("","Empty values are not allowed in parameter."),
        Arguments.of("  ","Empty values are not allowed in parameter."),
        Arguments.of("abc","Number format error ( not a valid distance)"),
        Arguments.of(";--AND XYZ=XYZ","Number format error ( not a valid distance)"),
        Arguments.of(random600Id,"Number format error ( not a valid distance)"),
        Arguments.of("%c2/_as=","Number format error ( not a valid distance)"),
        Arguments.of("5000","Distance greater than 1000.0 not allowed"),
        Arguments.of("3147483646","Invalid integer value (Integer overflow)."),
        Arguments.of("3147483647","Invalid integer value (Integer overflow)."));
  }
  
  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("distance parameter invalid values.")
  public void testInvalidDistanceValue(String value, String result, Vertx vertx,
      VertxTestContext testContext) {
    ValidationException ex = assertThrows(ValidationException.class, () -> {
      distanceTypeValidator.isValid(value);
    });
    assertEquals(result, ex.getMessage());
    testContext.completeNow();
  }
}
