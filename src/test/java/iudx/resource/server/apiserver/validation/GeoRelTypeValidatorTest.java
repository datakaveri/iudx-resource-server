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
import iudx.resource.server.apiserver.validation.types.GeoRelTypeValidator;

@ExtendWith(VertxExtension.class)
public class GeoRelTypeValidatorTest {

  private GeoRelTypeValidator geoRelTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }


  static Stream<Arguments> allowedValues() {
    // Add any valid value for which validation will pass successfully.
    return Stream.of(
        Arguments.of("within", true),
        Arguments.of("intersects", true),
        Arguments.of("near", true),
        Arguments.of(null, false));
  }


  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("georel parameter allowed values.")
  public void testValidGeoRelValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    geoRelTypeValidator = new GeoRelTypeValidator(value, required);
    assertTrue(geoRelTypeValidator.isValid());
    testContext.completeNow();
  }


  static Stream<Arguments> invalidValues() {
    // Add any invalid value for which validation must fail.
    String random600Id = RandomStringUtils.random(600);
    return Stream.of(
        Arguments.of("", true),
        Arguments.of("  ", true),
            Arguments.of("", false),
            Arguments.of("  ", false),
        Arguments.of("around", true),
        Arguments.of("bypass", true),
        Arguments.of("1=1", true),
        Arguments.of("AND XYZ=XYZ", true),
        Arguments.of(random600Id, true),
        Arguments.of("%2cX%2c", true));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("geo parameter invalid values.")
  public void testInvalidGeoRelValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    geoRelTypeValidator = new GeoRelTypeValidator(value, required);
    assertThrows(DxRuntimeException.class, () -> geoRelTypeValidator.isValid());
    testContext.completeNow();
  }

}
