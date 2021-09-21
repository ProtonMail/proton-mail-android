# Model class naming conventions

* Status: Discussed and agreed in principle
* Deciders: Marino, Zorica, Stefanija, Davide, Tomasz, Maciej
* Date: 20/08/2021


## Context and Problem Statement

We would like to have well defined conventions when it comes to model class naming in all the
application layers, for the sake of consistency.

## Considered Options

For the API models:
* `xRequest/xResponse` and `xResultResponse`, with an optional `xResultResponse` as a wrapper for the model, if needed,
  e.g. `GetMessageRequest`, `GetMessageResponse` and `GetMessageResultResponse` (contains `GetMessageResponse`),
* `xRequest/xApiModel` and `xResponse` as a wrapper for the model, if needed, 
  e.g. `GetMessageRequest`, `GetMessageApiModel` and `GetMessageResponse` (contains `GetMessageApiModel`).
  
For the DB models:
* `xEntity` e.g. `MessageEntity`,
* `xDatabaseModel` e.g. `MessageDatabaseModel`.

## Decision Outcome

* The presentation layer models will use `UiModel` suffix, e.g. `MessageUiModel`.
* The domain layer models will have no suffix, e.g. `Message`.
* The database models will have `Entity` suffix, e.g. `MessageEntity`.
* The API request models will have `Request` suffix, e.g. `GetMessageRequest`.
* The API response models will have `Response` suffix, e.g. `GetMessageResponse`.
* The API response wrapper models will have `ResultResponse` suffix, e.g. `GetMessageResultResponse`.

## Consequences
1. All the new models declared in the application should follow the conventions defined above.
