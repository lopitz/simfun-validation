package org.simfun.validation;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * <p>
 * This class is the entry point to build up a chain of validation. A functional map/flatMap-style or a imperative if-then-style might follow the validation.
 * The validation will be executed, after the chain has been built up with a call of validate().
 * </p>
 * <p></p>
 * <p>
 * <strong>Usage (functional):</strong>
 * <pre>
 * {@code
 * public Response doSomethingWithValue(String firstName, String lastName) {
 *   return Validation.of(firstName)
 *                    .with(Objects::nonNull, v -> "The parameter \"firstName\" must not be null.")
 *                    .with(StringUtils::isNotBlank)
 *                    .validate()
 *                    .flatMap(validatedValue -> Validation.of(lastName)
 *                                                         .with(Objects::nonNull, v -> "The parameter \"lastName\" must not be null.")
 *                                                         .validate())
 *                    .map(validatedValue -> successResponseFor(firstName, lastName))
 *                    .orElseGet((sourceValue, validationMessages) -> errorResponseFor(validationMessages));
 * }
 * }
 * </pre>
 * </p>
 * <p>
 * <strong>Usage (imperative):</strong>
 * <pre>
 * {@code
 * public Response doSomethingWithValue1(String firstName, String lastName) {
 *   var validationMessages = new ArrayList<String>();
 *   var validationResult = Validation.of(firstName)
 *                                    .withObjects::nonNull, v -> "The parameter \"firstName\" must not be null.")
 *                                    .with(StringUtils::isNotBlank)
 *                                    .validate();
 *
 *   if (validationResult.isSuccessful()) {
 *     var secondValidationResult = Validation.of(lastName)
 *                                            .with(Objects::nonNull, v -> The parameter \"lastName\" must not be null.)
 *                                            .validate();
 *     if (secondValidationResult.isSuccessful()) {
 *       return successResponseFor(firstName, lastName);
 *     }
 *     validationMessages.addAll(secondValidationResult.getMessages());
 *   }
 *   validationMessages.addAll(0, validationResult.getMessages());
 *   return errorResponseFor(validationMessages);
 * }
 * }
 * </pre>
 * </p>
 *
 * @param <T> Defines the type of the value that will be validated.
 */
public final class Validation<T> {

    private final T value;

    private Validation(T value) {
        this.value = value;
    }

    /**
     * Creates a new validation chain for the given value.
     *
     * @param value This is the value that will be validated.
     * @param <T>   Describes the type of the given value.
     * @return Returns new, empty validator chain for the given value.
     */
    public static <T> Validation<T> of(T value) {
        return new Validation<>(value);
    }

    /**
     * Adds a new validation into the chain. The check will be executed when the method {@code validate()} is called. As no error message is given the validation
     * will either use the exception message in case of an exception, or a generic fallback message if no exception was thrown within the check.
     *
     * @param check Contains the actual check that will be executed.
     * @return Returns the updated validator chain.
     */
    public ValidationStep<T> with(Predicate<T> check) {
        Objects.requireNonNull(check, "a check itself must not be null");
        return new ValidationStep<>(value, Validator.of(check));
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
        return new ValidationStep<>(value, Validator.of(check, errorMessageProvider));
    }
}
