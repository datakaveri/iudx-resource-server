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
import iudx.resource.server.apiserver.validation.types.GeoPropertyTypeValidator;

@ExtendWith(VertxExtension.class)
public class GeoPropertyTypeValidatorTest {

  private GeoPropertyTypeValidator geoPropertyTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("location", true), Arguments.of("Location", true), Arguments.of(null, false));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("GeoProperty parameter allowed values.")
  public void testValidGeoPropertyValue(
      String value, boolean required, Vertx vertx, VertxTestContext testContext) {
    geoPropertyTypeValidator = new GeoPropertyTypeValidator(value, required);
    assertTrue(geoPropertyTypeValidator.isValid());
    testContext.completeNow();
  }

  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    String random600Id = RandomStringUtils.random(600);
    return Stream.of(
        Arguments.of("", true),
        Arguments.of("  ", true),
        Arguments.of("around", true),
        Arguments.of("bypass", true),
        Arguments.of("1=1", true),
        Arguments.of("AND XYZ=XYZ", true),
        Arguments.of(random600Id, true),
        Arguments.of(null, true),
        Arguments.of("", false));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("GeoProperty parameter invalid values.")
  public void testInvalidGeoPropertyValue(
      String value, boolean required, Vertx vertx, VertxTestContext testContext) {
    geoPropertyTypeValidator = new GeoPropertyTypeValidator(value, required);
    assertThrows(DxRuntimeException.class, () -> geoPropertyTypeValidator.isValid());
    testContext.completeNow();
  }
}
