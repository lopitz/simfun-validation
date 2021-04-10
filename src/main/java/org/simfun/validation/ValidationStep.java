package org.simfun.validation;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ValidationStep<T> {

    private final T source;
    private final List<Validator<T>> validators;

    ValidationStep(T source, Validator<T> validator) {
        this.source = source;
        this.validators = List.of(validator);
    }

    private ValidationStep(T source, List<Validator<T>> validator) {
        this.source = source;
        this.validators = List.copyOf(validator);
    }

    /**
     * Adds a new validation into the chain. The check will be executed when the method {@code validate()} is called. As no error message is given the
     * validation will either use the exception message in case of an exception, or a generic fallback message if no exception was thrown within the check.
     *
     * @param check Contains the actual check that will be executed.
     * @return Returns the updated validator chain.
     */
    public ValidationStep<T> with(Predicate<T> check) {
        Objects.requireNonNull(check, "a check itself must not be null");
        return new ValidationStep<>(source, combineValidators(validators, Validator.of(check)));
    }

    /**
     * Adds a new validation into the chain. The check will be executed when the method {@code validate()} is called. If the validation fails the given error
     * message will be used.
     *
     * @param check Contains the actual check that will be executed.
     * @return Returns the updated validator chain.
     */
    public ValidationStep<T> with(Predicate<T> check, Function<T, String> errorMessageProvider) {
        Objects.requireNonNull(check, "a check itself must not be null");
        Objects.requireNonNull(errorMessageProvider, "the provider for the error message must not be null");
        return new ValidationStep<>(source, combineValidators(validators, Validator.of(check, errorMessageProvider)));
    }

    private List<Validator<T>> combineValidators(List<Validator<T>> validators, Validator<T> validator) {
        return Stream.of(validators, List.of(validator)).flatMap(Collection::stream).collect(Collectors.toList());
    }

    private List<String> completeValidation(T source) {
        return validators.stream()
                         .filter(validator -> !validator.validate(source))
                         .map(validator -> validator.getErrorMessage(source))
                         .collect(Collectors.toList());
    }

    /**
     * Executes all validation checks in the chain. If one of the checks fails, all other remaining checks will be executed. This enables building error
     * messages which contain all reasons why the given value is invalid and helps the customer to fix the parameters in as least circles as possible.
     *
     * @return Returns a validation result containing the state of the validation and error messages in case it was not successful.
     */
    public ValidationResult<T> validate() {
        return new ValidationResult<>(source, completeValidation(source));
    }
}
