package org.simfun.validation;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This class defines a validation to be executed on a given input value and an (optional) error message.
 * The error message can be omitted. If done so, the error message returned will be the message of a caught exception while in validation. If there is neither
 * an error message given, nor an exception caught, but the validation still not successful, a default message will be generated.
 *
 * @param <T> The type variable T defines the type of the value the validator will be applied on.
 */
public final class Validator<T> {

    private final Predicate<T> validator;
    private final Function<T, String> errorMessage;

    private String exceptionMessage;

    private Validator(Predicate<T> validator) {
        this.validator = validator;
        errorMessage = s -> unNull(exceptionMessage).isBlank() ? "validation failed for a value of type " + getClassName(s) : exceptionMessage;
    }

    private Validator(Predicate<T> validator, Function<T, String> errorMessage) {
        this.validator = validator;
        this.errorMessage = errorMessage;
    }

    public static <T> Validator<T> of(Predicate<T> validator) {
        return new Validator<>(validator);
    }

    public static <T> Validator<T> of(Predicate<T> validator, Function<T, String> errorMessage) {
        return new Validator<>(validator, errorMessage);
    }

    public boolean validate(T source) {
        try {
            return validator.test(source);
        } catch (Exception e) {
            exceptionMessage = e.getMessage();
            return false;
        }
    }

    public String getErrorMessage(T source) {
        return errorMessage.apply(source);
    }

    private static String unNull(String source) {
        if (source == null) {
            return "";
        }
        return source;
    }

    private static String getClassName(Object object) {
        if (object == null) {
            return "<null>";
        }
        return object.getClass().getName();
    }
}
