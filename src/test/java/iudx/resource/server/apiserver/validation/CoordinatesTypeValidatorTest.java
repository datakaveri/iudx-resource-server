package iudx.resource.server.apiserver.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.stream.Stream;
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
import iudx.resource.server.apiserver.validation.types.CoordinatesTypeValidator;

@ExtendWith(VertxExtension.class)
public class CoordinatesTypeValidatorTest {

  private ParameterTypeValidator coordinatesTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    coordinatesTypeValidator = new CoordinatesTypeValidator().create();
    testContext.completeNow();
  }


  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("[21.178,72.834]"),
        Arguments.of(
            "[[[72.719,21],[72.842,21.2],[72.923,20.8],[72.74,20.34],[72.9,20.1],[72.67,20],[72.719,21]]]"),
        Arguments.of("[[72.8296,21.2],[72.8297,21.15]]"),
        Arguments.of("[[72.842,21.2],[72.923,20.8],[72.74,20.34],[72.9,20.1],[72.67,20]]"));
  }


  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("coordinates type parameter allowed values.")
  public void testValidCoordinatesTypeValue(String value, Vertx vertx,
      VertxTestContext testContext) {
    RequestParameter result = coordinatesTypeValidator.isValid(value);
    assertEquals(value, result.getString());
    testContext.completeNow();
  }


  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("", "Empty value not allowed for parameter."),
        Arguments.of("  ", "Empty value not allowed for parameter."),
        Arguments.of("[21.1784567787,72.834]", "invalid coordinate (only 6 digits to precision allowed)"),
        Arguments.of("[21.17,72.83467867874564]", "invalid coordinate (only 6 digits to precision allowed)"),
        Arguments.of("[21.178,72.834,23.5678]", "Invalid number of coordinates given for point"),
        Arguments.of("[21178345635353535353534521312,72.8342]",
            "invalid longitude value 21178345635353535353534521312"),
        Arguments.of("[21.17834,7283425675567567567567567567567]",
            "invalid latitude value 7283425675567567567567567567567"),
        Arguments.of(
            "[[[73.8444,18.5307],[73.84357,18.52820],[73.8492,18.52836],[73.84632,18.52250],[73.83816,18.52934],[73.83576,18.52063],[73.84357,18.51631],[73.84992,18.51338],[73.85833,18.51672],[73.86065,18.52185],[73.861770,18.52567],[73.85928,18.52966],[73.85447,18.53414],[73.8444,18.53072]]]",
            "Invalid numbers of coordinates supplied (Only 10 coordinates allowed for polygon and line & 1 coordinate for point)"),
        Arguments.of(
            "[[[73.8425,18.52791],[73.8411,18.5194],[73.8541,18.51880],[73.85892,18.5275],[73.84250,18.5279235684]]]",
            "invalid coordinate (only 6 digits to precision allowed)"));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("coordinates type parameter invalid values.")
  public void testInvalidCoordinatesTypeValue(String value, String result, Vertx vertx,
      VertxTestContext testContext) {
    ValidationException ex = assertThrows(ValidationException.class, () -> {
      coordinatesTypeValidator.isValid(value);
    });
    assertEquals(result, ex.getMessage());
    testContext.completeNow();
  }

}
