# Use Markdown Architectural Decision Records

* Status: Accepted
* Deciders: Marino, Zorica, Stefanija, Davide, Tomasz, Dimitar, Nikola, Denys
* Date: 13/11/2020


## Context and Problem Statement

There are several very explicit goals that make the practice and discipline of architecture very important:

* We want to think deeply about all our architectural decisions, exploring all alternatives and making a careful, considered, well-researched choice.
* We want to be as transparent as possible in our decision-making process.
* We don't want decisions to be made unilaterally in a vacuum. Specifically, we want to give our steering group the opportunity to review every major decision.
* Despite being a geographically and temporally distributed team, we want our contributors to have a strong shared understanding of the technical rationale behind decisions.
* We want to be able to revisit prior decisions to determine fairly if they still make sense, and if the motivating circumstances or conditions have changed.
* We want each developer in the company, new or old, to be have clear references to the decisions that were taken in the past and are currently in force in the project.

**Which format and structure should these records follow?**

## Considered Options

* [MADR](https://adr.github.io/madr/) 2.1.0 - The Markdown Architectural Decision Records
* [Michael Nygard's template](http://thinkrelevance.com/blog/2011/11/15/documenting-architecture-decisions) - The first incarnation of the term "ADR"
* [Sustainable Architectural Decisions](https://www.infoq.com/articles/sustainable-architectural-design-decisions) - The Y-Statements
* Other templates listed at <https://github.com/joelparkerhenderson/architecture_decision_record>
* Formless - No conventions for file format and structure

## Decision Outcome

Chosen option: "MADR 2.1.0", because

* Implicit assumptions should be made explicit.
Design documentation is important to enable people understanding the decisions later on.
See also [A rational design process: How and why to fake it](https://doi.org/10.1109/TSE.1986.6312940).
* The MADR format is lean and fits our development style.
* The MADR structure is comprehensible and facilitates usage & maintenance.
* The MADR project is vivid.
* Version 2.1.0 is the latest one available when starting to document ADRs.

The workflow will be:

* A developer creates an ADR document outlining an approach for a particular question or problem. The ADR has an initial status of "proposed."
* The developers and steering group discuss the ADR. During this period, the ADR should be updated to reflect additional context, concerns raised, and proposed changes.
* Once consensus is reached, ADR can be transitioned to either an "accepted" or "rejected" state.
* Only after an ADR is accepted should implementing code be committed to the master branch of the relevant project/module.
* If a decision is revisited and a different conclusion is reached, a new ADR should be created documenting the context and rationale for the change. The new ADR should reference the old one, and once the new one is accepted, the old one should (in its "status" section) be updated to point to the new one. The old ADR should not be removed or otherwise modified except for the annotation pointing to the new ADR.

## Consequences
1. Developers must write an ADR and submit it for review before selecting an approach to any architectural decision -- that is, any decision that affects the way ProtonMail application is put together at a high level.
2. We will have a concrete artifact around which to focus discussion, before finalizing decisions.
3. If we follow the process, decisions will be made deliberately, as a group.
4. The develop branch of our repositories will reflect the high-level consensus of the steering group.
5. We will have a useful persistent record of why the system is the way it is.

## Links
* [Confluence page on ADR](https://confluence.protontech.ch/pages/viewpage.action?pageId=24117283)
* [Arachne Framework sample ADR](https://github.com/arachne-framework/architecture/blob/master/adr-001-use-adrs.md)
* [MADR ADR template](https://adr.github.io/madr/)
