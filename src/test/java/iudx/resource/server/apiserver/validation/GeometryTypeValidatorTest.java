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
import iudx.resource.server.apiserver.validation.types.GeometryTypeValidator;

@ExtendWith(VertxExtension.class)
public class GeometryTypeValidatorTest {

  private ParameterTypeValidator geomTypeValidator;
  
  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    geomTypeValidator = new GeometryTypeValidator().create();
    testContext.completeNow();
  }
  
  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("point"),
        Arguments.of("Point"),
        Arguments.of("polygon"),
        Arguments.of("Polygon"),
        Arguments.of("LineString"),
        Arguments.of("linestring"),
        Arguments.of("bbox"));
  }
  
  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("geometry type parameter allowed values.")
  public void testValidGeomTypeValue(String value, Vertx vertx, VertxTestContext testContext) {
    RequestParameter result = geomTypeValidator.isValid(value);
    assertEquals(value, result.getString());
    testContext.completeNow();
  }
  
  
  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    String random600Id=RandomStringUtils.random(600);
    return Stream.of(
        Arguments.of("","Empty value not allowed for parameter."),
        Arguments.of("  ","Empty value not allowed for parameter."),
        Arguments.of("around", "Value around is not allowed"),
        Arguments.of("bypass", "Value bypass is not allowed"),
        Arguments.of("1=1", "Value 1=1 is not allowed"),
        Arguments.of("AND XYZ=XYZ", "Value AND XYZ=XYZ is not allowed"),
        Arguments.of(random600Id, "Value " + random600Id + " is not allowed"),
        Arguments.of("%2cX%2c", "Value %2cX%2c is not allowed"));
  }
  
  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("geometry type parameter invalid values.")
  public void testInvalidGeomTypeValue(String value, String result, Vertx vertx,
      VertxTestContext testContext) {
    ValidationException ex = assertThrows(ValidationException.class, () -> {
      geomTypeValidator.isValid(value);
    });
    assertEquals(result, ex.getMessage());
    testContext.completeNow();
  }
}
