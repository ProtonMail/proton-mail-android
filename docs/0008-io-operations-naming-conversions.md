# I/O operations naming conventions

* Status: Accepted
* Deciders: Marino, Zorica, Stefanija, Davide, Tomasz, Maciej
* Date: 22/09/2021


## Context and Problem Statement

We would like to have well defined conventions about naming for I/O functions ( mainly DAOs, APIs, Repositories, Use Cases ), for the sake of consistency.

## Considered Options

For **DAOs**:

* `get-`
* `find-`
* `fetch-`

For **APIs**

* `get-`
* `fetch-`

For higher levels ( **repositories, use cases, workers** )

* `get-` / `get~Once`
* `observe-`

## Decision Outcome

Every component will respect the global conventions of the underlying language ( SQL for database and HTTP for network )

For **DAOs**

* `find-` for single item
* `findAll-` for many items
* `insert-` / `update-` / `set-` for write

For **APIs**

* `get-` for single and many items
* `put-` / `post-` 

For higher levels ( **repositories, use cases, workers** )

* `get-` to *get* a single event ( see suspend function )
* `observe-` to *observe* a stream of events ( see Coroutines Flow )
* Common sense general developing names for actions / write ( like `moveMessages`, `markAsRead`, etc )

## Consequences
1. All the new I/O functions declared in the application should follow the conventions defined above
2. Whenever possible, we should rename the pre-existen ones
