# Use UserNotifier component to show user feedback from workers

* Status: Accepted
* Deciders: Marino, Zorica, Stefanija, Davide, Tomasz, Dimitar
* Date: 09/02/2021


## Context and Problem Statement

Given this app is structured to allow all operations to be performed independently from nework availability,
most of the business and data logic is somehow wrapped into components that handle the part of waiting for
network (such as jobs and workers).

Having this logic executed asynchronously, in an undefined moment and in the background, represents a challenge
when it comes to showing feedback to the user in regards of the operation that was executed.

* We want to have a consistent and reliable way to inform the user of any important event that happened in a background Worker (eg. failed to send)
* We can't rely on the app's UI existing as each worker's execution time depends on external factors (the app might have been killed by the time they execute)
* We want the ability to fully unit test that this actions happened

## Considered Options

* Inject an android collaborator to display local push notifications
* Return feedback to the caller (use case) and allow them to handle it
* Inject an abstract collaborator that can decide how to display feedback (UserNotifier)

## Decision Outcome

* We choose to rely on a small, simple and abstract `UserNotifier` component
* This component gets injected in a worker and called when feedback needs to be shown
* UserNotifier decides how to display feedback, for example through a Push notification or a Snackbar or a Toast

## Consequences
1. All Workers and Jobs that need to show user-visible feedback should do so through an injected instance of UserNotifier component

