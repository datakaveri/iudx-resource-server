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
import iudx.resource.server.apiserver.validation.types.AttrsTypeValidator;

@ExtendWith(VertxExtension.class)
public class AttrsTypeValidatorTest {

  private AttrsTypeValidator attrsTypevalidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("refrenceLeval,Co2,NO2,SO2,CO", true),
        Arguments.of(null, false));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("Attrs type parameter allowed values.")
  public void testValidAttrsTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    attrsTypevalidator = new AttrsTypeValidator(value, required);
    assertTrue(attrsTypevalidator.isValid());
    testContext.completeNow();
  }

  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("", true),
        Arguments.of("  ", true),
        Arguments.of("refrenceLeval,Co2,NO2,SO2,CO,ABC", true),
        Arguments.of(RandomStringUtils.random(102) + ",refrenceLeval,Co2,NO2,SO2", true),
        Arguments.of("refrence$Leval,Co2,NO2,SO2", true),
        Arguments.of("refrenceLeval,Co2,NO2,S*&O2", true),
            Arguments.of("",false),
            Arguments.of("",true),
            Arguments.of(null,true));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("Attrs type parameter invalid values.")
  public void testInvalidAttrsTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    attrsTypevalidator = new AttrsTypeValidator(value, required);
    assertThrows(DxRuntimeException.class, () -> attrsTypevalidator.isValid());
    testContext.completeNow();
  }

}
