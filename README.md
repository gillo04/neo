# Neo
Neo aims at being a RISC-V out-of-order soft core written in Chisel.

# Project status
At the moment, there is a working in-order pipeline both with and without register renaming. The current implementations can execute all RV32I instructions, except for `FENCE`, `FENCE.TSO`, `PAUSE`, `ECALL` and `EBREAK`.
The general timeline looks as follows:
- [x] Implement an in-order pipeline
- [x] Implement a reorder buffer and make a pipeline with register renaming
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

# Synthesis
Here are some statistics for the synthesis of each core. These come from synthesizing the corse for the Ultra96 V2 embedded board.
| Core | LUTs | Regisers |
| --- | --- | --- |
| `in_order` | 1605 | 1306 |
| `reorder` | 5708 | 4115 |
| `out_of_order` | - | - |
