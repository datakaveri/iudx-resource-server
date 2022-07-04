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
import iudx.resource.server.apiserver.validation.types.IDTypeValidator;

@ExtendWith(VertxExtension.class)
public class IdTypeValidatorTest {

  private IDTypeValidator idTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }


  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta",
            true),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055",
            true),
        Arguments.of(null, false));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("geometry type parameter allowed values.")
  public void testValidIDTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    idTypeValidator = new IDTypeValidator(value, required);
    assertTrue(idTypeValidator.isValid());
    testContext.completeNow();
  }

  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    String random600Id = RandomStringUtils.random(600);
    return Stream.of(
        Arguments.of("", true),
        Arguments.of("  ", true),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta/sasd asdd",
            true),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta AND 2434=2434 AND 'qLIl'='qLIl",
            true),
        Arguments.of("bypass", true),
        Arguments.of("1=1", true),
        Arguments.of("AND XYZ=XYZ", true),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/"
                + random600Id,
            true),
        Arguments.of("%2cX%2c", true),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta$",
            true),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information$/surat-itms-live-eta",
            true),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io$/surat-itms-realtime-information/surat-itms-live-eta",
            true),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86$/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta",
            true),
            Arguments.of("",true),
            Arguments.of(null,true),
            Arguments.of("",false));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("id type parameter invalid values.")
  public void testInvalidIDTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    idTypeValidator = new IDTypeValidator(value, required);
    assertThrows(DxRuntimeException.class, () -> idTypeValidator.isValid());
    testContext.completeNow();
  }
}
