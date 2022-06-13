package iudx.resource.server.apiserver.validation;

import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.apiserver.validation.types.OptionsHeaderValidator;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class OptionsHeaderValidatorTest {

    private OptionsHeaderValidator optionsHeaderValidator;

    @BeforeEach
    public void setup(Vertx vertx, VertxTestContext testContext){testContext.completeNow();}

    static Stream<Arguments> allowedValues(){
        //Values which will pass successfully
        return Stream.of(
                Arguments.of("streaming",true),
                Arguments.of(null,false)
        );

    }

    @ParameterizedTest
    @MethodSource("allowedValues")
    @Description("option header allowed values.")
    public void testValidOptionHeaderValue(String value, boolean required, Vertx vertx, VertxTestContext testContext){
        optionsHeaderValidator =new OptionsHeaderValidator(value,required);
        assertTrue(optionsHeaderValidator.isValid());
        testContext.completeNow();
    }

    static Stream<Arguments> invalidValues() {
        // Add any valid value which will not pass successfully.
        String random600Id = RandomStringUtils.random(600);
        return Stream.of(
                Arguments.of("", true),
                Arguments.of("  ", true),
                Arguments.of("around", true),
                Arguments.of("bypass", true),
                Arguments.of("1=1", true),
                Arguments.of("AND XYZ=XYZ", true),
                Arguments.of(random600Id, true),
                Arguments.of("%2cX%2c", true),
                Arguments.of(" "," "));
    }

    @ParameterizedTest
    @MethodSource("invalidValues")
    @Description("option value invalid values.")
    public void testInvalidOptionHeadervalue(String value, boolean required, Vertx vertx,VertxTestContext testContext){
        optionsHeaderValidator=new OptionsHeaderValidator(value,required);
        assertThrows(DxRuntimeException.class,()->optionsHeaderValidator.isValid());
        testContext.completeNow();
    }



}
