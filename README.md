# Neo
Neo aims at being a RISC-V out-of-order soft core written in Chisel.

# Project status
At the moment, only the in-order pipeline has been implemented, while the renaming pipeline is a work in progress. The current implementations can execute all RV32I instructions, except for `FENCE`, `FENCE.TSO`, `PAUSE`, `ECALL` and `EBREAK`.
The general timeline looks as follows:
- [x] Implement an in-order pipeline
- [ ] Implement a reorder buffer and make a pipeline with register renaming
- [ ] Implement reservation stations and make an out-of-order pipeline
- [ ] Implement brach prediction. Probably a simple global-history branch predictor
- [ ] Implement caching
- [ ] Implement load-store queues

# Run it
You can run all tests by executing:
```
sbt test
``` 
You may see some tests failing due to them accessing the same test binary at the same time. You can test the individual pipelines by running:
```
sbt "testOnly in_order.*"
sbt "testOnly reorder.*"
``` 
You can generate the verilog for the integrated in-order pipeline by executing:
``` 
sbt run
```
The resulting files will be placed in the `./builds/` direcotry.
