# Use backticks for jUnit tests' naming

* Status: Accepted
* Deciders: Marino, Zorica, Stefanija, Davide, Maciej
* Date: 27/10/2021


## Context and Problem Statement

We want a format that empowers readability of test cases, that doesn't make this decay when we add many informations.

## Considered Options

* Keep the current `camelCase` format
* **Switch to use `` `backticks format` ``**

* Current format has been chosen in order to keep consistency with instrumented tests, which don't support backticks

## Decision Outcome

We decided that backticks is the most human readable format, that enables us to add many information into the test case's name, without sacrificing the readability.

## Consequences
1. All the new test cases will use the backticks format
2. When adding new test cases to a pre-existing class that uses camel case, the developer must evaluate whether is feasible to rename not compliant pre-existing test cases
