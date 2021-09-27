# Use functions over annotations for creating mocks

* Status: Accepted
* Deciders: Marino, Zorica, Stefanija, Davide, Tomasz, Maciej
* Date: 22/09/2021


## Context and Problem Statement

We would like to have a consistent way of declaring mocks in the tests that will aid readability and
clarity.

## Considered Options

* Using a `mockk()` function and defining the mock's default behaviour when it's first declared.
* Using a `@MockK` annotation in combination with `@BeforeTest` annotated function to init the mocks
and define their default behaviour.

## Decision Outcome

In order to maximise the readability and clarity of the test code, `mockk()` function has been 
chosen as the preferred solution; defining the mock's default behaviour in the same place it is
declared, makes it more transparent and harder to overlook.

## Consequences
1. All the new tests will use the `mockk()` function approach over the alternative.
