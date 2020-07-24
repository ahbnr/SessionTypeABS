The session types used by this tool and their syntax are explained in the thesis
linked in the main readme.

However, this is a short overview of the ASCII-based representation of the
global session types which must be used when supplying a session specification
to this tool:

| Type                                     | Representation         |
|:-----------------------------------------|:-----------------------|
| ![](readme_data/initialization_type.png) | `0 -f-> P:m<pure-exp>` |
| ![](readme_data/interaction_type.png)    | `P -f-> Q:m<pure-exp>` |
| ![](readme_data/resolving_type.png)      | `P resolves f with C`  |
| ![](readme_data/fetching_type.png)       | `P fetches f as C`     |
| ![](readme_data/releasing_type.png)      | `Rel(P, f)`            |
| ![](readme_data/skip_type.png)           | `skip`                 |
| ![](readme_data/branching_type.png)      | `P{...}`               |
| ![](readme_data/repeating_type.png)      | `(...)*`               |
| ![](readme_data/concatenation_type.png)  | `G1.G2`                |

See also the examples at `evaluation/models/simple`.

