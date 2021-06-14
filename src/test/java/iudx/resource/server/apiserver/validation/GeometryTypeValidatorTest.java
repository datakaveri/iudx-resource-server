package iudx.resource.server.apiserver.validation;

import static org.junit.jupiter.api.Assertions.*;
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
import iudx.resource.server.apiserver.validation.types.GeometryTypeValidator;

@ExtendWith(VertxExtension.class)
public class GeometryTypeValidatorTest {

  private GeometryTypeValidator geomTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("point", true),
        Arguments.of("Point", true),
        Arguments.of("polygon", true),
        Arguments.of("Polygon", true),
        Arguments.of("LineString", true),
        Arguments.of("linestring", true),
        Arguments.of("bbox", true),
        Arguments.of(null, false));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("geometry type parameter allowed values.")
  public void testValidGeomTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    geomTypeValidator = new GeometryTypeValidator(value, required);
    assertTrue(geomTypeValidator.isValid());
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
        Arguments.of("%2cX%2c", true));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("geometry type parameter invalid values.")
  public void testInvalidGeomTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    geomTypeValidator = new GeometryTypeValidator(value, required);
    assertFalse(geomTypeValidator.isValid());
    testContext.completeNow();
  }
}
