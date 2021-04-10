package org.simfun.validation;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ValidationTest {

    private static final int FORTY_YEARS = 40;
    private static final int SEVENTEEN_YEARS = 17;
    private static final int MIN_AGE = 18;

    @Nested
    class InvalidArguments {

        @Test
        void shouldRejectNullValueOfAValidator() {
            var validation = Validation.of("value to check");

            assertThatCode(() -> validation.with(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("a check itself must not be null");
        }

        @Test
        void shouldRejectNullValueOfAnyValidator() {
            var validation = Validation
                .of("value to check")
                .with(valueToCheck -> true);

            assertThatCode(() -> validation.with(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("a check itself must not be null");
        }

        @Test
        void shouldRejectNullValueOfAMessageProvider() {
            var validation = Validation.of("value to check");

            assertThatCode(() -> validation.with(value -> true, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("the provider for the error message must not be null");
        }

        @Test
        void shouldRejectNullValueOfAnyMessageProvider() {
            var validation = Validation
                .of("value to check")
                .with(valueToCheck -> true, valueToCheck -> "");

            assertThatCode(() -> validation.with(value -> true, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("the provider for the error message must not be null");
        }

        @Test
        void shouldAcceptNullInValueToBeValidated() {
            assertThatCode(() -> Validation.of(null).with(valueToCheck -> true).validate()).doesNotThrowAnyException();
        }
    }

    @Nested
    class Mapping {

        @Test
        void shouldReturnDefaultValueOnValidationErrors() {
            var user = new User(null, null, null, 0);
            var validation = Validation
                .of(user)
                .with(u -> u.getUsername() != null, u -> "username must not be null")
                .with(u -> u.getFirstName() != null, u -> "first name must not be null")
                .validate()
                .map(u -> true)
                .orElseGet((u, errors) -> false);

            assertThat(validation).isFalse();
        }

        @Test
        void shouldReturnMappedValueOnSuccessfulValidation() {
            var user = new User(null, null, null, 0);
            var validation = Validation
                .of(user)
                .with(u -> u.getUsername() == null, u -> "username must not be null")
                .with(u -> u.getFirstName() == null, u -> "first name must not be null")
                .validate()
                .map(u -> true)
                .orElseGet((u, errors) -> false);

            assertThat(validation).isTrue();
        }

        @Test
        void shouldNotCallFlatMapOnFailOfFirstValidation() {
            var user = new User("username", "firstname", "Lastname", FORTY_YEARS);

            var validation = Validation
                .of(user)
                .with(Objects::nonNull)
                .validate()
                .flatMap(u -> Validation
                    .of(user.firstName)
                    .with(name -> Character.isUpperCase(name.charAt(0)), name -> String.format("[%s] doesn't start with an upper case letter", name))
                    .validate())
                .orElseGet((firstName, errors) -> "test successful, validation failed");

            assertThat(validation).isEqualTo("test successful, validation failed");
        }

        @Test
        void shouldCallFlatMapOnSuccessOfFirstValidation() {
            var user = new User("username", "Firstname", "Lastname", FORTY_YEARS);

            var validation = Validation
                .of(user)
                .with(Objects::nonNull)
                .validate()
                .flatMap(u -> Validation
                    .of(user.firstName)
                    .with(name -> Character.isUpperCase(name.charAt(0)), name -> String.format("[%s] doesn't start with an upper case letter", name))
                    .validate());

            assertThat(validation.isSuccessful()).isTrue();
            assertThat(validation.getMessages()).isEmpty();
        }

        @Test
        void shouldCollectAllErrorsOfFlatMappedValidation() {
            var user = new User("username", "Firstname", "Lastname", FORTY_YEARS);

            var validation = Validation
                .of(user)
                .with(Objects::nonNull)
                .validate()
                .flatMap(u -> Validation
                    .of(user.firstName)
                    .with(name -> Character.isLowerCase(name.charAt(0)), name -> String.format("[%s] doesn't start with a lower case letter", name))
                    .with(name -> Character.isUpperCase(name.charAt(name.length() - 1)), name -> String.format("[%s] doesn't end with an upper case letter", name))
                    .validate());

            assertThat(validation.isSuccessful()).isFalse();
            assertThat(validation.getMessages())
                .containsExactly(
                    "[Firstname] doesn't start with a lower case letter",
                    "[Firstname] doesn't end with an upper case letter"
                );
        }

        @Test
        void shouldSupportFlatMap() {
            var validation = Validation
                .of((User) null)
                .with(Objects::isNull)
                .validate()
                .map(u -> "bla")
                .flatMap(u -> Validation
                    .of(u)
                    .with(Objects::nonNull)
                    .validate()
                    .map(a -> "blubber"))
                .orElseGet((u, l) -> "didn't work");

            assertThat(validation).isEqualTo("blubber");
        }

        @Test
        void shouldNotCallMapperInMapOnValidationError() {
            var innerValidation = new ValidatorFake<User, String>();
            Validation
                .of((User) null)
                .with(Objects::nonNull)
                .validate()
                .map(innerValidation)
                .orElseGet((u, l) -> "didn't work");
            assertThat(innerValidation.wasCalled()).isFalse();
        }

        @Test
        void shouldNotCallMapperInFlatMapOnValidationError() {
            var innerValidation = new ValidatorFake<String, ValidationResult<String>>();
            Validation
                .of((User) null)
                .with(Objects::nonNull)
                .validate()
                .map(u -> "bla")
                .flatMap(innerValidation)
                .orElseGet((u, l) -> "didn't work");
            assertThat(innerValidation.wasCalled()).isFalse();
        }

        @Test
        void shouldProvideErrorMessageOfFlatMappedValidation() {
            var v = Validation
                .of("Blubber")
                .with(Objects::nonNull)
                .validate()
                .map(u -> "bla")
                .flatMap(u -> Validation.of(u).with("Blubber"::equals, a -> "great").validate())
                .getMessages();
            assertThat(v).isNotEmpty().contains("great");
        }
    }

    @Nested
    class ValidationResults {

        @Test
        void shouldHaveNoValidationMessagesOnSuccessfulValidation() {
            var user = new User(null, null, null, 0);
            var v = Validation
                .of(user)
                .with(u -> u.getUsername() == null, u -> "username must not be null")
                .with(u -> u.getFirstName() == null, u -> "username must not be null")
                .validate();

            assertThat(v.isSuccessful()).isTrue();
            assertThat(v.getMessages()).isEmpty();
        }

        @Test
        void shouldHaveValidationMessageForEachFailedValidation() {
            var user = new User(null, null, null, 0);
            var v = Validation
                .of(user)
                .with(u -> Objects.nonNull(u.getUsername()), u -> "username must not be null")
                .with(u -> u.getUsername() != null, u -> "username must not be null")
                .with(u -> u.getUsername() == null, u -> "username must be null")
                .validate();

            assertThat(v.isSuccessful()).isFalse();
            assertThat(v.getMessages()).hasSize(2);
        }

        @Test
        void shouldUseGivenErrorMessageOnException() {
            var v = Validation
                .of((User) null)
                .with(u -> {
                    throw new NullPointerException("test");
                }, u -> "username must not be null")
                .validate();

            assertThat(v.isSuccessful()).isFalse();
            assertThat(v.getMessages()).contains("username must not be null");
        }

        @Test
        void shouldUseExceptionMessageIfNoDefaultMessageIsGivenOnException() {
            var v = Validation
                .of((User) null)
                .with(u -> {
                    throw new NullPointerException("test");
                })
                .validate();

            assertThat(v.isSuccessful()).isFalse();
            assertThat(v.getMessages()).contains("test");
        }

        @Test
        void shouldUseFallbackMessageIfNoDefaultMessageIsGivenOnNonExceptionButFailedValidation() {
            var v = Validation
                .of("Blubber")
                .with("Bla"::equals)
                .validate();

            assertThat(v.isSuccessful()).isFalse();
            assertThat(v.getMessages()).hasSize(1).contains("validation failed for a value of type java.lang.String");
        }

        @Test
        void shouldUseFallbackMessageIfNoDefaultMessageIsGivenOnNonExceptionButFailedValidationOnNullValue() {
            var v = Validation
                .of((User) null)
                .with(u -> false)
                .validate();

            assertThat(v.isSuccessful()).isFalse();
            assertThat(v.getMessages()).hasSize(1).contains("validation failed for a value of type <null>");
        }

        @Test
        void shouldCheckUsersAge() {
            var user = new User("youngster", null, "Young", 17);
            var v = Validation
                .of(user)
                .with(u -> u.getAge() > 17, invalidUser -> String.format("User %s is too young [age: %d]", invalidUser.getUsername(), invalidUser.getAge()))
                .with(u -> Objects.nonNull(u.getFirstName()))
                .validate();

            assertThat(v.isSuccessful()).isFalse();
            assertThat(v.getMessages())
                .containsExactly(
                    "User youngster is too young [age: 17]",
                    "validation failed for a value of type org.simfun.validation.ValidationTest$User");
        }

        @Test
        void shouldBeAbleToTakeInLists() {
            var valid = new Person("John Doe", 30);
            var invalid = new Person("John? Doe!4", -1);

            var values = List.of(valid, invalid);

            var validValues = values
                .stream()
                .map(person -> Validation
                    .of(person)
                    .with(checkedPerson -> checkedPerson.getName().matches("[a-zA-Z ]+"), invalidPerson -> "Name contains invalid characters")
                    .with(checkedPerson -> MIN_AGE < checkedPerson.getAge(), invalidPerson -> "Age must be at least " + MIN_AGE)
                    .validate())
                .filter(ValidationResult::isSuccessful)
                .map(ValidationResult::get)
                .collect(Collectors.toList());

            assertThat(validValues).containsExactly(valid);
        }
    }

    private static class ValidatorFake<T, R> implements Function<T, R> {

        private boolean called = false;

        public boolean wasCalled() {
            return called;
        }

        @Override
        public R apply(T t) {
            called = true;
            return null;
        }
    }

    private static class User {
        private final String username;
        private final String firstName;
        private final String lastName;
        private final int age;

        public User(String username, String firstName, String lastName, int age) {
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
            this.age = age;
        }

        public String getUsername() {
            return username;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public int getAge() {
            return age;
        }
    }

    private static class Person {
        private final String name;
        private final int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }
}
