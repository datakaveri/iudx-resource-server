package iudx.resource.server.apiserver.validation;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.apiserver.validation.types.HeaderKeyTypeValidation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class HeaderKeyTypeValidationTest {

private static HeaderKeyTypeValidation validation;

    public static Stream<Arguments> inputValues()
    {
        return Stream.of(
        Arguments.of(null,false),
        Arguments.of("I6W24wZTKcH0Xl6ykwD8eSLv8EZIhPa2WkwYzmZzP10=",true),
        Arguments.of("nKU4Mdtq01N9BY_AAu5Hr_ha8IFuqWMfhWiBRBhb4lA=",true)
        );
    }

    @ParameterizedTest
    @MethodSource("inputValues")
    @DisplayName("Test isValid method : Success")
    public void testIsValid(String value, boolean required, VertxTestContext vertxTestContext)
    {
        validation = new HeaderKeyTypeValidation(value,required);
        assertTrue(validation.isValid());
        vertxTestContext.completeNow();
    }

    public static Stream<Arguments> invalidInputValues()
    {
        return Stream.of(
                Arguments.of(null,true),
                Arguments.of("",true),
                Arguments.of("",false),
                Arguments.of("some public key value",true),
                Arguments.of("I6W24wZTKcH0Xl6ykwD8eSLv8EZIhPa2WkwYz+/++10",true)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidInputValues")
    @DisplayName("Test isValid method : Failure")
    public void testIsValidFailure(String value, boolean required, VertxTestContext vertxTestContext)
    {
        validation = new HeaderKeyTypeValidation(value,required);
        assertThrows(DxRuntimeException.class,()-> validation.isValid());
        vertxTestContext.completeNow();
    }



}
