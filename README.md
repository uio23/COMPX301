# NPCStack
**Author**: Oleksandr Kashpir</br>
_**ID**_: 1637705
---
## Compilation
The algorithm is implemented in a single source code file, `NPCStack.java`. To compile it, just run:
```
javac NPCStack.java
```
## Usage
`java NPCStack filename.txt initialTemp coolingRate [nerf]`

This program can be used in one of two ways:
```
java NPCStack filename.txt initialTemp coolingRate
```
To output, to standard output, the tallest stack that my program can find, leveraging dynamic programming and annealing, for a set of boxes specified in the given file.
```
java NPCStack filename.txt initialTemp coolingRate nerf
```
To perform the same annealing search, but scramble the initial sequence of boxes selected with the help of a dynamic programming algorithm. Specifically, 1/4 of the boxes will be changed, if possible, before annealing is run.
## Terminology
For the purposes of this documentation:
- A box is a line from the specified file.
- A rotation is one of the 3 possible rotations of a box.
- A cuboid is any rotation of any box.
- A box is in a stack if its contained by a Stack object, and its actually part of the tallest stack sequence if its "included" in the stack. Hopefully this distinction is clear contextually.
- A change a box is to either remove or include a single one of its rotations in the tallest stack sequence, or to change the rotation included.
- The touching faces constraint is that a cuboid may only go on top of another cuboid if its width and length are strictly smaller than those of the cuboid that it is going on top of.
- The single-use constraint is that a box can only be present in the stack once, as a cuboid, in the form of one of its rotations.
## Notable algorithm features
I have implemented a dynamic programming algorithm to find the tallest stack sequence, and then reduce the resulting stack to only include the tallest rotations of each box in it.
My annealing algorithm makes temperature-many changes to this sequence, accepting the changes if they increase the height of the stack, and reduces the temperature by the cooling rate after every such cycle.
Once the temperature, which is always rounded up from a double value, reaches 0, the annealing stops and the Stack object's sequence is displayed.

The actual changes are overseen by the Stack object, which tries to make changes to the specified number of unique boxes, where each change is restricted to the single use constraint and validated against the touching faces constraint.
I say tries to, because some number of changes to a stack may not always be possible, but to ensure the maximum number of changes does get applied, all the boxes in the stack are repeatedly tried for a change, in a random order, over and over until no change is made during a pass through all the boxes.

Another implication of my approach to changing the sequence is that it does not make sense/is not possible to change more boxes than are present. Therefore, I have imposed a limit on the temperature to be less than or equal to the number of boxes, i.e. the number of cuboids / 3.
