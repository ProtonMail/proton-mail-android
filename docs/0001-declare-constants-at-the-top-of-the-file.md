# Constants are created by file as a top level declaration

* Status: Accepted
* Deciders: Marino, Zorica, Stefanija, Davide, Tomasz, Maciej
* Date: 17/09/2021


## Context and Problem Statement

This is a minor code-style decision regarding where constants should be placed.


## Considered Options

1. Declare constants at the top of the file (right after imports)
```
import ...

const val PUBLIC_CONSTANT="value"
private const val CONSTANT="value"

class { ... }
```

2. Declare constants at a file's level but at the bottom
3. Declare constants in a companion object, at the bottom of the file

## Decision Outcome
Accepting the risks of name clashes that might turn out from making some private constants public when they are declared outside of companion objects (as evaluated low probability-low impact), we decided to go for Option #1.
Such decison was based on the fact that we didn't see major differencies between the listed approaches and approach #1 is already the one with the wider usage in the codebase.

