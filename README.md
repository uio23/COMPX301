# XSort
**Author**: Oleksandr Kashpir</br>
_**ID**_: 1637705
---
## Compilation
The algorithm is implemented in a single source code file, `XSort.java`. To compile it, just run:
```
javac XSort.java
```
## Usage
`java XSort runLength [2]`

This program can be used in one of two ways:
```
cat input.txt|java XSort [runLength (between 64 and 1024 inclusive)] > output.txt
```
To read lines from standard in, and output lexicographically sorted initial runs to standard out as they are generated.
```
cat input.txt|java XSort [runLength] 2 > output.txt
```
To perform a balanced 2-way merge of the initial runs and output the final, single, lexicographically sorted run of lines to standard out.
## Notable algorithm features
This algorithm is an external balanced 2-way sort merge.

Instead of counting the run length for every merge to a particular output tape, my algorithm merges runs into a run on a tape until the lexicographically smaller of the lines from the two input files is smaller than the previously written line. This indicates that a run boundary that cannot be crossed has been reached, and a new run must be created in the next output tape, after flushing values from the other input tape, until the same boundary condition is met there.
This means that at worst, log2R merge cycles will need to take place, but for every run that happens to lexicographically follow on from the previous run in ascending order, the number of output runs is reduced by 1/2, such that if in some 2 input tapes with r runs, r/2 runs follow the preceding run, then the number of runs in the following cycle will be r/2-(1/2*r/2) = r/2-r/4 = r/4, skipping a merge cycle!

This approach requires me to perform an additional comparison for each input line, comparing with the previously written line. Since this is in place of a 'current run length' check for every line merge, and since this algorithm allows for the (albeit unlikely) skip of a merge cycle, I believe that's alright.

Furthermore, this 'previous line' must be initialised such that no input line could be less than it, so I initialise it to an empty String, and reset it to that when switching tapes to write a new run.
## Accreditation
As referenced in the appropriate section of the source code, the heapsort functions were implemented with reference to [Interview Cake](https://www.interviewcake.com/concept/java/heapsort)
