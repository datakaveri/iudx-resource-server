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
import iudx.resource.server.apiserver.validation.types.AttrsTypeValidator;

@ExtendWith(VertxExtension.class)
public class AttrsTypeValidatorTest {

  private ParameterTypeValidator attrsTypevalidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    attrsTypevalidator = new AttrsTypeValidator().create();
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("refrenceLeval,Co2,NO2,SO2,CO"));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("Attrs type parameter allowed values.")
  public void testValidAttrsTypeValue(String value, Vertx vertx, VertxTestContext testContext) {
    RequestParameter result = attrsTypevalidator.isValid(value);
    assertEquals(value, result.getString());
    testContext.completeNow();
  }

  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("", "Empty value not allowed for parameter."),
        Arguments.of("  ", "Empty value not allowed for parameter."),
        Arguments.of("refrenceLeval,Co2,NO2,SO2,CO,ABC", "More than 5 attributes are not allowed."),
        Arguments.of(RandomStringUtils.random(102) + ",refrenceLeval,Co2,NO2,SO2",
            "One of the attribute exceeds allowed characters(only 100 characters allowed)."));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("Attrs type parameter invalid values.")
  public void testInvalidAttrsTypeValue(String value, String result, Vertx vertx,
      VertxTestContext testContext) {
    ValidationException ex = assertThrows(ValidationException.class, () -> {
      attrsTypevalidator.isValid(value);
    });
    assertEquals(result, ex.getMessage());
    testContext.completeNow();
  }

}
