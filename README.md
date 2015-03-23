## Project Description

This is the codes for CF'15 paper "An Evaluation and Analysis of Graph Processing Frameworks on Five Key Issues".

With the continuously emerging applications in fields like social media analysis, mining massive graphs has drawn increasing attentions from industry and academia. To aid the development of distributed graph algorithms, various programming frameworks have been proposed. To better understand their performance differences under specific scenarios, we analyzed and compared a set of seven representative frameworks under five design aspects, including distribution policy, on-disk data organization, programming model, synchronization policy and message model. Our experiments reveal some interesting phenomena. For example, We observed that the vertex-cut method overweighs the edge-cut method on neighbor-based algorithms while leads to inefficiency for non-neighbor-based algorithms. Furthermore, we observed that using asynchronous update can reduce the total workload by 20% to 30%, but the processing time may 
still doubled due to fine-grained lock conflicts. Overall, we analyzed the pros and cons of each option for the five key issues. We believe our findings will help end-users choose a suitable framework,and designers improve current ones. 


## Directories

datasets: Our scripts to fetch dataset and transform it to our input form from law.di.unimi.it/datasets.php

framewors: We modified the framework source codes to output execution times of specific actions, memory consumption and some other variables. On consideration of the project size, we only upload the source codes. However, the directories is the same, users could download original programs and substitute the source codes with rsync.

algorithms: The implementations of our benchmark algorithms on each framework.

scripts: Our scripts to automatically run algorithms on a series of input graphs, and transform the added outputs to human-readable format.
