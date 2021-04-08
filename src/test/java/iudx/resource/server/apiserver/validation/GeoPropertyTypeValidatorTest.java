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
import iudx.resource.server.apiserver.validation.types.GeoPropertyTypeValidator;
import iudx.resource.server.apiserver.validation.types.TimeRelTypeValidator;

@ExtendWith(VertxExtension.class)
public class GeoPropertyTypeValidatorTest {

  private ParameterTypeValidator geoPropertyTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    geoPropertyTypeValidator = new GeoPropertyTypeValidator().create();
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("location"),
        Arguments.of("Location"));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("GeoProperty parameter allowed values.")
  public void testValidGeoPropertyValue(String value, Vertx vertx, VertxTestContext testContext) {
    RequestParameter result = geoPropertyTypeValidator.isValid(value);
    assertEquals(value, result.getString());
    testContext.completeNow();
  }
  
  
  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    String random600Id=RandomStringUtils.random(600);
    return Stream.of(
        Arguments.of("","Empty/Null value not allowed."),
        Arguments.of("  ","Empty/Null value not allowed."),
        Arguments.of("around","Only location is allowed for geoproperty"),
        Arguments.of("bypass","Only location is allowed for geoproperty"),
        Arguments.of("1=1","Only location is allowed for geoproperty"),
        Arguments.of("AND XYZ=XYZ","Only location is allowed for geoproperty"),
        Arguments.of(random600Id,"Only location is allowed for geoproperty"));
  }
  
  
  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("GeoProperty parameter invalid values.")
  public void testInvalidGeoPropertyValue(String value, String result, Vertx vertx,
      VertxTestContext testContext) {
    ValidationException ex = assertThrows(ValidationException.class, () -> {
      geoPropertyTypeValidator.isValid(value);
    });
    assertEquals(result, ex.getMessage());
    testContext.completeNow();
  }

}
