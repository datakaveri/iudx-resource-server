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
import iudx.resource.server.apiserver.validation.types.IDTypeValidator;

@ExtendWith(VertxExtension.class)
public class IdTypeValidatorTest {

  private ParameterTypeValidator idTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    idTypeValidator = new IDTypeValidator().create();
    testContext.completeNow();
  }


  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055"));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("geometry type parameter allowed values.")
  public void testValidIDTypeValue(String value, Vertx vertx, VertxTestContext testContext) {
    RequestParameter result = idTypeValidator.isValid(value);
    assertEquals(value, result.getString());
    testContext.completeNow();
  }

  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    String random600Id = RandomStringUtils.random(600);
    return Stream.of(
        Arguments.of("", "Empty value not allowed."),
        Arguments.of("  ", "Empty value not allowed."),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta/sasd asdd",
            "Invalid id."),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta AND 2434=2434 AND 'qLIl'='qLIl",
            "Invalid id."),
        Arguments.of("bypass", "Invalid id."),
        Arguments.of("1=1", "Invalid id."),
        Arguments.of("AND XYZ=XYZ", "Invalid id."),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/"
                + random600Id,
            "Value exceed max character limit."),
        Arguments.of("%2cX%2c", "Invalid id."));
  }
  
  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("id type parameter invalid values.")
  public void testInvalidIDTypeValue(String value, String result, Vertx vertx,
      VertxTestContext testContext) {
    ValidationException ex = assertThrows(ValidationException.class, () -> {
      idTypeValidator.isValid(value);
    });
    assertEquals(result, ex.getMessage());
    testContext.completeNow();
  }
}
