package iudx.resource.server.apiserver.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import iudx.resource.server.apiserver.validation.types.QTypeValidator;

@ExtendWith(VertxExtension.class)
public class QTypeValidatorTest {

  private ParameterTypeValidator qTypeValidator;

  static Stream<Arguments> values() {
    // Add any invalid value which will throw error.
    return Stream.of(
        Arguments.of("","Empty value not allowed for parameter."),
        Arguments.of("    ","Empty value not allowed for parameter."),
        Arguments.of(RandomStringUtils.random(600),"Exceeding max length(512 characters) criteria"),
        Arguments.of("referenceLevel<>15.0", "Operator not allowed."),
        Arguments.of("referenceLevel>>15.0", "Operator not allowed."),
        Arguments.of("referenceLevel===15.0", "Operator not allowed."),
        Arguments.of("referenceLevel+15.0", "Operator not allowed."),
        Arguments.of("referenceLevel/15.0", "Operator not allowed."),
        Arguments.of("referenceLevel*15.0", "Operator not allowed."));
  }


  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    qTypeValidator = new QTypeValidator().create();
    testContext.completeNow();
  }

  @ParameterizedTest
  @MethodSource("values")
  @Description("q parameter type failure for different invalid values.")
  public void testInvalidQTypeValue(String value, String result, Vertx vertx,
      VertxTestContext testContext) {
    ValidationException ex = assertThrows(ValidationException.class, () -> {
      qTypeValidator.isValid(value);
    });
    assertEquals(result, ex.getMessage());
    testContext.completeNow();
  }
  
  @Test
  @Description("success for valid q query")
  public void testValidQValue(Vertx vertx,VertxTestContext testContext) {
    RequestParameter result=qTypeValidator.isValid("referenceLevel>15.0");
    assertEquals("referenceLevel>15.0", result.getString());
    testContext.completeNow();
  }


}
