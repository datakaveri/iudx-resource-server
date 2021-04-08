package iudx.resource.server.apiserver.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
//import org.junit.jupiter.params.provider.MethodSource;
import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.validation.types.OptionsTypeValidator;

@ExtendWith(VertxExtension.class)
public class OptionsTypeValidatorTest {

  private ParameterTypeValidator optionsValidator;

  static Stream<Arguments> values() {
    //Add any invalid value  which will  throw  error.
    return Stream.of(
        Arguments.of("count1"),
        Arguments.of("AND 1=1"),
        Arguments.of("1==1"));
  }

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    optionsValidator = new OptionsTypeValidator().create();
    testContext.completeNow();

  }

  @ParameterizedTest
  @MethodSource("values")
  @Description("options parameter type failure for different invalid values.")
  public void testInvalidOptionsValue(String value, Vertx vertx, VertxTestContext testContext) {
    ValidationException ex = assertThrows(ValidationException.class, () -> {
      optionsValidator.isValid(value);
    });
    assertEquals("count is only allowed value for options parameter", ex.getMessage());
    testContext.completeNow();
  }
  
  @Test
  @Description("success for valid options")
  public void testValidOptionsValue(Vertx vertx,VertxTestContext testContext) {
    RequestParameter result=optionsValidator.isValid("count");
    assertEquals("count", result.getString());
    testContext.completeNow();
  }
  
}
