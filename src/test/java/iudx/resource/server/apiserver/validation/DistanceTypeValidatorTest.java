package iudx.resource.server.apiserver.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
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
import iudx.resource.server.apiserver.validation.types.DistanceTypeValidator;

@ExtendWith(VertxExtension.class)
public class DistanceTypeValidatorTest {

  private DistanceTypeValidator distanceTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
            Arguments.of("1", true),
            Arguments.of("500", true),
            Arguments.of(null, false));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("distance parameter allowed values.")
  public void testValidDistanceValue(String value, boolean required, Vertx vertx,
                                     VertxTestContext testContext) {
    distanceTypeValidator = new DistanceTypeValidator(value, required);
    assertTrue(distanceTypeValidator.isValid());
    testContext.completeNow();
  }


  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    String random600Id = RandomStringUtils.random(600);
    return Stream.of(
            Arguments.of("", true),
            Arguments.of("  ", true),
            Arguments.of("abc", true),
            Arguments.of(";--AND XYZ=XYZ", true),
            Arguments.of(random600Id, true),
            Arguments.of("%c2/_as=", true),
            Arguments.of("5000", true),
            Arguments.of("-1", true),
            Arguments.of("3147483646", true),
            Arguments.of("3147483647", true),
            Arguments.of(null,true),
            Arguments.of("",false));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("distance parameter invalid values.")
  public void testInvalidDistanceValue(String value, boolean required, Vertx vertx,
                                       VertxTestContext testContext) {
    distanceTypeValidator = new DistanceTypeValidator(value, required);
    assertThrows(DxRuntimeException.class, () -> distanceTypeValidator.isValid());
    testContext.completeNow();
  }


}