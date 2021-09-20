# Use runBlockingTest

* Status: Accepted
* Deciders: Marino, Zorica, Stefanija, Davide, Tomasz, Maciej
* Date: 10/09/2021


## Context and Problem Statement

Inconstancy in test between the usage of `runBlocking` and `runBlockingTest`

## Considered Options
* `runBlocking`
* `runBlockingTest`

#### Evaluations between `runBlocking` and `runBlockingTest`

* Both of the solutions executes all the Coroutines on a single thread
* `runBlockingTest` is specifically **designed for test**, while `runBlocking` has a different purpose
* `runBlockingTest` enables us to control the **virtual time**, if needed
* `runBlockingTest` has auto-advance features, that let's us **skip the various `delay()`** and similar that would let the tests take a considerable amount of time to be completed
* `runBlockingTest` helps us to verify that the Coroutines has a **correct flow** ( e.g. does not "get stuck" or never completes ) and **does not leak**
* `runBlockingTest` has **some incompatibilities**, for example with Room's Transactions

## Decision Outcome

* We should use `runBlockingTest` as a standard
* We can use `runBlockingWithTimeout` when there is a incompatibility issue ( see Room's Transactions ) and the test cannot completes correctly otherwise

## Consequences
1. We should replace `runBlocking` with `runBlockingTest` wherever possible
2. We should replace `runBlocking` with `runBlockingWithTimeout` when `runBlockingTest` cannot be used
3. Further discussion is needed for `coroutinesTest`
