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
import iudx.resource.server.apiserver.validation.types.QTypeValidator;

@ExtendWith(VertxExtension.class)
public class QTypeValidatorTest {

  private QTypeValidator qTypeValidator;

  static Stream<Arguments> invalidValues() {
    // Add any invalid value which will throw error.
    return Stream.of(
        Arguments.of("", true),
        Arguments.of("    ", true),
        Arguments.of(RandomStringUtils.random(600), true),
        Arguments.of("referenceLevel<>15.0", true),
        Arguments.of("referenceLevel>>15.0", true),
        Arguments.of("referenceLevel===15.0", true),
        Arguments.of("referenceLevel+15.0", true),
        Arguments.of("referenceLevel/15.0", true),
        Arguments.of("referenceLevel*15.0", true),
        Arguments.of("reference_Level$>15.0", true),
        Arguments.of("reference$Level>15.0", true),
        Arguments.of("referenceLevel!<15.0", true));
  }


  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("q parameter type failure for different invalid values.")
  public void testInvalidQTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    qTypeValidator = new QTypeValidator(value, required);
    assertThrows(DxRuntimeException.class, () -> qTypeValidator.isValid());
    testContext.completeNow();
  }

  static Stream<Arguments> validValues() {
    return Stream.of(
        Arguments.of("referenceLevel>15.0", true),
        Arguments.of("reference_Level>15.0", true),
        Arguments.of(
            "id==iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055",
            true),
        Arguments.of(null, false));
  }

  @ParameterizedTest
  @MethodSource("validValues")
  @Description("success for valid q query")
  public void testValidQValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    qTypeValidator = new QTypeValidator(value, required);
    assertTrue(qTypeValidator.isValid());
    testContext.completeNow();
  }


}
