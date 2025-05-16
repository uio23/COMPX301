**Author**: Oleksandr Kashpir
_**ID**_: 1637705</br>
**Partner**: Hiran Greening
_**ID**_: 1522172</br>
# REsearch
---
## Compilation
The searcher implementation is done with multiple classes in a single source code file, `REsearch.java`. To compile it, just run:
```
javac REsearch.java
```
## Usage
This program must receive lines through standard input to load a FSM from. These can be supplied in one of two recommended ways:
```
java REcompile "regexp" | java REsearch filename.txt
```
To pipe them from the standard output of my partner's program for a specified regular expression "regexp".
```
cat FSMlines.txt | java REcompile filename.txt
```
To instead pipe them from a text file.
## Comments
My implementation of a Deque is developed to work around a persistent SCAN node storing a SCAN value, which is defined by the constructor upon initialization. In my searcher, I chose to instantiate my DequeWithSCAN with a SCAN value of -2, as -1 should be treated as a state number (representing the final state by agreement between my and my partner Hiran).</br>
Another significant assumption my searcher makes about the FSM it receives is that a literal or WC cannot branch, meaning that both of its next states are the same. Me and Hiran discussed this assumption and we are in agreement on it.
