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
import iudx.resource.server.apiserver.validation.types.GeoRelTypeValidator;

@ExtendWith(VertxExtension.class)
public class GeoRelTypeValidatorTest {

  private ParameterTypeValidator geoRelTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    geoRelTypeValidator = new GeoRelTypeValidator().create();
    testContext.completeNow();
  }


  static Stream<Arguments> allowedValues() {
    // Add any valid value for which validation will pass successfully.
    return Stream.of(
        Arguments.of("within"),
        Arguments.of("intersects"),
        Arguments.of("near"));
  }


  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("georel parameter allowed values.")
  public void testValidGeoRelValue(String value, Vertx vertx, VertxTestContext testContext) {
    RequestParameter result = geoRelTypeValidator.isValid(value);
    assertEquals(value, result.getString());
    testContext.completeNow();
  }


  static Stream<Arguments> invalidValues() {
    // Add any invalid value for which validation must fail.
    String random600Id = RandomStringUtils.random(600);
    return Stream.of(
        Arguments.of("", "Empty value not allowed for parameter."),
        Arguments.of("  ", "Empty value not allowed for parameter."),
        Arguments.of("around", "Value around is not allowed"),
        Arguments.of("bypass", "Value bypass is not allowed"),
        Arguments.of("1=1", "Value 1=1 is not allowed"),
        Arguments.of("AND XYZ=XYZ", "Value AND XYZ=XYZ is not allowed"),
        Arguments.of(random600Id, "Value " + random600Id + " is not allowed"),
        Arguments.of("%2cX%2c", "Value %2cX%2c is not allowed"));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("geo parameter invalid values.")
  public void testInvalidGeoRelValue(String value, String result, Vertx vertx,
      VertxTestContext testContext) {
    ValidationException ex = assertThrows(ValidationException.class, () -> {
      geoRelTypeValidator.isValid(value);
    });
    assertEquals(result, ex.getMessage());
    testContext.completeNow();
  }

}
