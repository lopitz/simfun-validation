<!-- this README is based on the wonderful work by Othneil Drew at https://github.com/othneildrew/Best-README-Template -->

[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![Apache 2.0 License][license-shield]][license-url]
[![LinkedIn][linkedin-shield]][linkedin-url]

# SimFun Validation

<!-- TABLE OF CONTENTS -->
<details open="open">
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about">About The Project - or simply "WHY???"</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgements">Acknowledgements</a></li>
  </ol>
</details>

## OMG, yet another validation framework - WHY???
<a name="about"></a>

Ok, ok. I must admit, there are a gazillion of validation frameworks out there. However, none of them really fits my needs. And here, is why:

```java

@RestController
@RequestMapping("/users")
public class UserController {

    @PostMapping
    public Mono<UserDto> storeUser(@RequestBody UserDto userDto) {
        return Validation
            .of(userDto)
            .with(Objects::nonNull)
            .with(user -> Objects.nonNull(user.username()))
            .with(user -> user.age() >= ADULT_LEGAL_AGE, user -> "The user is too young.")
            .validate()
            .map(userGateway::storeUser)
            .orElseGet((result, errors) -> Mono.error(() -> new UserValidationException(errors)));
    }
}
```

As you can see, the validation definition is clear and concise. The functional approach lets you build up pipelines which are executed depending on the
validation results.

### Comparison simfun-validation to VAVR

Let's compare this lib to VAVR, which also contains a validation piece:

#### VAVR

```java

public class Vavr {

    public void validate() {
        PersonValidator personValidator = new PersonValidator();

        // Valid(Person(John Doe, 30))
        Validation<Seq<String>, Person> valid = personValidator.validatePerson("John Doe", 30);

        // Invalid(List(Name contains invalid characters: '!4?', Age must be greater than 0))
        Validation<Seq<String>, Person> invalid = personValidator.validatePerson("John? Doe!4", -1);
        //A valid value is contained in a Validation.Valid instance, a list of validation errors is contained in a Validation.Invalid instance.
    }
    
    //The following validator is used to combine different validation results to one Validation instance.
    class PersonValidator {

        private static final String VALID_NAME_CHARS = "[a-zA-Z ]";
        private static final int MIN_AGE = 0;

        public Validation<Seq<String>, Person> validatePerson(String name, int age) {
            return Validation.combine(validateName(name), validateAge(age)).ap(Person::new);
        }

        private Validation<String, String> validateName(String name) {
            return CharSeq.of(name).replaceAll(VALID_NAME_CHARS, "").transform(seq -> seq.isEmpty()
                ? Validation.valid(name)
                : Validation.invalid("Name contains invalid characters: '"
                + seq.distinct().sorted() + "'"));
        }

        private Validation<String, Integer> validateAge(int age) {
            return age < MIN_AGE
                ? Validation.invalid("Age must be at least " + MIN_AGE)
                : Validation.valid(age);
        }
    }
}
```

#### simfun-validation

```java
public class SimFunValidation {

    public void validate() {

        var valid = new Person("John Doe", 30);
        var invalid = new Person("John? Doe!4", -1);

        var validValues = List.of(valid, invalid)
            .stream()
            .map(person -> Validation
                .of(person)
                .with(checkedPerson -> checkedPerson.getName().matches("[a-zA-Z ]+"), invalidPerson -> "Name contains invalid characters")
                .with(checkedPerson -> MIN_AGE < checkedPerson.getAge(), invalidPerson -> "Age must be at least " + MIN_AGE)
                .validate())
            .filter(ValidationResult::isSuccessful)
            .map(ValidationResult::get)
            .collect(Collectors.toList());
    }
}
```

### Built With

The project uses plain java without any other dependency pulled in. However, it uses Java 11 features (`var`) and thus can only be used in projects that use
Java 11 or above by themselves.

<!-- GETTING STARTED -->

## Getting Started

### Prerequisites

This lib uses Java 11 features. Thus, your project needs to be using at least Java 11. A backport is possible (replace `var` all over the place) - but hey, why?

### Installation

Just declare a new dependency in your dependency management system as such:

#### Maven

```xml

<dependency>
    <groupId>org.simfun</groupId>
    <artifactId>validation</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Gradle

```groovy
dependencies { testImplementation "org.simfun:validation:1.0.0" }
```

<!-- USAGE EXAMPLES -->

## Usage

### The functional approach

```java

@RestController
@RequestMapping("/users")
public class UserController {

    @PostMapping
    public Mono<UserDto> storeUser(@RequestBody UserDto userDto) {
        return Validation
            .of(userDto)
            .with(Objects::nonNull)
            .with(user -> Objects.nonNull(user.username()))
            .with(user -> user.age() >= ADULT_LEGAL_AGE, user -> "The user is too young.")
            .validate()
            .map(userGateway::storeUser)
            .orElseGet((result, errors) -> Mono.error(() -> new UserValidationException(errors)));
    }
}
```

### The more traditional-Java approach

```java

@RestController
@RequestMapping("/users")
public class UserController {

    @PostMapping
    public UserDto storeUser(@RequestBody UserDto userDto) {
        var validationResult = Validation
            .of(userDto)
            .with(Objects::nonNull)
            .with(user -> Objects.nonNull(user.username()))
            .with(user -> user.age() >= ADULT_LEGAL_AGE)
            .validate();

        if (validationResult.isSuccessful()) {
            return userGateway.storeUser(userDto);
        }

        throw new UserValidationException(validationResult.getMessages());
    }
}
```

The validation lib takes in any value, even `null`, as a value to be checked. However, it will reject `null` as value for a check and will also reject `null`
for an error message provider.

**Allowed**

```java
    var validation=Validation.of(null);
```

**Throws a NullPointerException**

```java
    var badCheckUsage=Validation
    .of("")
    .with(null);  //NPE thrown here due to check being null

    var badMessageProviderUsage=Validation
    .of("")
    .with(valueToCheck->true,null);  //NPE thrown here due to error message provider being null
```

### Combining Validations

```java

@RestController
@RequestMapping("/users")
public class UserController {

    @PostMapping
    public Mono<UserDto> storeUser(@RequestBody UserDto userDto) {
        return Validation
            .of(user)
            .with(Objects::nonNull)
            .validate()
            .flatMap(user -> Validation
                .of(user.firstName)
                .with(name -> Character.isLowerCase(name.charAt(0)), name -> String.format("[%s] doesn't start with a lower case letter", name))
                .with(name -> Character.isUpperCase(name.charAt(name.length() - 1)), name -> String.format("[%s] doesn't end with an upper case letter", name))
                .validate()
                .map(firstName -> userDto))
            .map(userGateway::storeUser)
            .orElseGet((result, errors) -> Mono.error(() -> new UserValidationException(errors)));
    }
}
```

<!-- ROADMAP -->

## Roadmap

See the [open issues](https://github.com/lopitz/simfun-validation/issues) for a list of proposed features (and known issues).



<!-- CONTRIBUTING -->

## Contributing

Contributions are what make the open source community such an amazing place to be, learn, inspire, and create. Any contributions you make are **greatly
appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

Please find more information in [CONTRIBUTING.md](CONTRIBUTING.md)

<!-- LICENSE -->

## License

Distributed under the MIT License. See [LICENSE.md](LICENSE.md) for more information.


<!-- CONTACT -->

## Contact

Your Name - [@lars_opitz](https://twitter.com/lars_opitz)

Project Link: [https://github.com/lopitz/simfun-validation](https://github.com/lopitz/simfun-validation)


<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->

[contributors-shield]: https://img.shields.io/github/contributors/lopitz/simfun-validation.svg?style=flat-square

[contributors-url]: https://github.com/lopitz/simfun-validation/graphs/contributors

[forks-shield]: https://img.shields.io/github/forks/lopitz/simfun-validation.svg?style=flat-square

[forks-url]: https://github.com/lopitz/simfun-validation/network/members

[stars-shield]: https://img.shields.io/github/stars/lopitz/simfun-validation.svg?style=flat-square

[stars-url]: https://github.com/lopitz/simfun-validation/stargazers

[issues-shield]: https://img.shields.io/github/issues/lopitz/simfun-validation.svg?style=flat-square

[issues-url]: https://github.com/lopitz/simfun-validation/issues

[license-shield]: https://img.shields.io/github/license/lopitz/simfun-validation.svg?style=flat-square

[license-url]: https://github.com/lopitz/simfun-validation/blob/master/LICENSE.txt

[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=flat-square&logo=linkedin&colorB=555

[linkedin-url]: https://www.linkedin.com/in/larsopitz/
