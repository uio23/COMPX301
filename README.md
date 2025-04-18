# KMPsearch
**Author**: Oleksandr Kashpir</br>
_**ID**_: 1637705
---
## Compilation
The algorithm is implemented in a single source code file, `KMPsearch.java`. To compile it, just run:
```
javac KMPsearch.java
```
## Usage
`java KMPsearch "target" filename.txt`

This program can be used in one of two ways:
```
java KMPsearch "target"
```
To output, to standrd output, the KMP skip table for the given target string.
```
java KMPsearch "target" filename.txt
```
To perform Knuth-Morris-Pratt string search on the specified file and output, to standard output, those lines from the file that have at least one occurance of the target substring, preceded by: a 1-based index into that line where the pattern first occures, followed by a space.
