package iudx.resource.server.apiserver.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.apiserver.validation.types.ElasticSearchFileResponseTypeValidator;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestElasticSearchFileResponseTypeValidator {
  ElasticSearchFileResponseTypeValidator typeValidator;

  static Stream<Arguments> input() {
    return Stream.of(
        Arguments.of(null, false),
        Arguments.of("csv", false),
        Arguments.of("parquet", false),
        Arguments.of("json", false),
        Arguments.of("csv", true),
        Arguments.of("parquet", true),
        Arguments.of("json", true));
  }

  static Stream<Arguments> inputValues() {

    return Stream.of(
        Arguments.of("HTML", false),
        Arguments.of("", true),
        Arguments.of(null, true),
        Arguments.of("", false));
  }

  @ParameterizedTest
  @DisplayName("Test isValid method : Success")
  @MethodSource("input")
  public void testIsValid(String value, boolean required, VertxTestContext vertxTestContext) {
    typeValidator = new ElasticSearchFileResponseTypeValidator(value, required);
    assertTrue(typeValidator.isValid());
    vertxTestContext.completeNow();
  }

  @ParameterizedTest
  @DisplayName("Test isValid method : Failure")
  @MethodSource("inputValues")
  public void testIsValidFailure(
      String value, boolean required, VertxTestContext vertxTestContext) {
    typeValidator = new ElasticSearchFileResponseTypeValidator(value, required);
    assertThrows(DxRuntimeException.class, () -> typeValidator.isValid());
    vertxTestContext.completeNow();
  }
}
