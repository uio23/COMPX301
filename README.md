**Author**: Oleksandr Kashpir
_**ID**_: 1637705</br>
**Partner**: Hiran Greening
_**ID**_: TBC</br>
# REsearch
---
## Compilation
The searcher implemention is done with multiple classes in a single source code file, `REsearch.java`. To compile it, just run:
```
javac REsearch.java
```
## Usage
This program must recieve lines through standard input to load a FSM from. These can be supplied in one of two recommended ways:
```
java REcompile "regexp" | java REsearch filename.txt
```
To pipe them from the standard output of my partner's program for a specified regular expression "regexp".
```
cat FSMlines.txt | java REcompile filename.txt
```
To instead pipe them from a text file.
## Comments
My implementation of a Deque is developed to work around a persistent SCAN node storing a SCAN value, which is defined by the constructor upon initialization. In my searcher, I chose to instantiate my DequeWithSCAN using the value of -2 for this, as -1 should be treated as a state number (representing the final state by agreement between my and Hiran).
