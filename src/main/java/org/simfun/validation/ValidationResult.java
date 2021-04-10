package org.simfun.validation;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class ValidationResult<T> {

    private final T source;
    private final boolean successful;
    private final List<String> validationMessages;

    ValidationResult(T source, List<String> validationMessages) {
        this.source = source;
        this.successful = validationMessages.isEmpty();
        this.validationMessages = validationMessages;
    }

    /**
     * If and only if the validation was successful and no validation errors were raised, the given mapper function will be executed on the value given to the
     * validation. The mapper result can be accessed then using {@link #orElseGet(BiFunction)}.
     *
     * @param mapper Contains the mapping function which will be executed with the value given to the validation, in case the validation was successful.
     * @param <U>    Describes the type of the desired transformation.
     * @return a validation result containing the value of the applied mapper function.
     * @see java.util.Optional#map(Function)
     */
    public <U> ValidationResult<U> map(Function<? super T, ? extends U> mapper) {
        return new ValidationResult<>(provideSource(mapper).orElse(null), validationMessages);
    }

    /**
     * If and only if the validation was successful and no validation errors were raised, the given mapper function will be executed on the value given to the
     * validation. The result of the mapping function which is a validation result by itself will be unpacked from the created validation result. So instead of
     * having a validation result with the newly calculated value inside another validation result there will be only one validation result with the newly
     * created value.
     * The mapper result can be accessed then using {@link #orElseGet(BiFunction)}.
     *
     * @param mapper Contains the mapping function which will be executed with the value given to the validation, in case the validation was successful.
     * @param <U>    Describes the type of the desired transformation.
     * @return a validation result containing the value of the applied mapper function.
     * @see java.util.Optional#flatMap(Function)
     */
    @SuppressWarnings("unchecked")
    public <U> ValidationResult<U> flatMap(Function<? super T, ? extends ValidationResult<? extends U>> mapper) {
        return ((Optional<ValidationResult<U>>) provideSource(mapper)).orElse(new ValidationResult<>(null, validationMessages));
    }

    private <U> Optional<U> provideSource(Function<? super T, ? extends U> provider) {
        if (successful) {
            return Optional.ofNullable(provider.apply(source));
        }
        return Optional.empty();
    }

    /**
     * If the validation was successful it returns the value of the validation chain.
     * Due to potential mapping in-between it is not necessarily the validated value.
     * In case the original value given into the validation was invalid, a NoSuchElementException will be thrown instead.
     *
     * @return the current value of the validation.
     */
    public T get() {
        return provideSource(t -> t).orElseThrow();
    }

    /**
     * If the validation was successful it returns the value of the validation chain. Due to potential mapping in-between it is not necessarily the validated
     * value. In case the original value given into the validation was invalid, the value calculated by the passed in function will be return instead.
     *
     * @param defaultValue This is the mapper function that creates a default value. The input parameters for this function are the current value within this
     *                     validation result and the list with the error messages from the validation.
     * @return Returns the current value of the validation or a default value in case the value passed into the validation is invalid.
     */
    public T orElseGet(BiFunction<T, List<String>, T> defaultValue) {
        return provideSource(s -> s).orElse(defaultValue.apply(source, validationMessages));
    }

    /**
     * Returns whether the validation was successful, meaning the value passed into the validation was valid.
     *
     * @return {@code true}, if the validated value was valid otherwise {@code false}.
     */
    public boolean isSuccessful() {
        return successful;
    }

    /**
     * Returns all the messages of failed validations within the validation chain.
     *
     * @return a list containing all error messages given by failed validations.
     */
    public List<String> getMessages() {
        return validationMessages;
    }

}
