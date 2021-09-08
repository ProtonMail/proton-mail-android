# Use Mappers that extends core Mapper

* Status: Discussed and agreed in principle
* Deciders: Marino, Zorica, Stefanija, Davide, Tomasz, Maciej
* Date: 31/08/2021


## Context and Problem Statement

Currently in the application we have different approaches for mapping between clean architecture
layers and models, these approaches vary in terms of complexity and readability. Going forward,
especially having in mind current conversion to core networking and database models we would like 
to have common approach to this basic problem.

* We want to have a consistent approach to mappers in the application
* We want the ability to fully unit test mappers and code related

## Considered Options

* Mapper based on kotlin extension functions e.g. ConversationMapper with ConversationApiModel.toLocal(userId: String)
* Old school mapper (java style) with functions like .mapResponseToEntity(response: Response)
* Mapper that extends interface Mapper<in In, out Out>  from core, and implements methods as extension function e.g. fun BusinessModel.toUiModel(): UiModel
* Mapper that extends interface Mapper<in In, out Out>  from core, with plain function per parameter e.g. fun toUiModel(type: BusinessModel): UiModel

## Decision Outcome

* We choose to rely on a mapper that extends interface Mapper<in In, out Out>  from core, with plain function per parameter
* We would like to use one Mapper per 2 types, therefore multiple mappers may be required
* We should not use extension function from Core, about `List` and `Flow`, but the standard syntax ( e.g. `list.map { mapper.toUiModel(it) }` or `mapper.toUiModels(list)` )

## Consequences
1. All Workers mappers in the application should follow the approach suggested above

