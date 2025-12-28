import chisel3._
import chisel3.util._

class InOrder extends Module {
  val io = IO(new Bundle{
    // Icache
    val inst_in = Input(UInt(32.W))
    val pc =      Output(UInt(32.W))

    // Debug
    val rf =      Output(Vec(32, UInt(32.W)))
    val stall =   Output(Bool())

    val s1_p0 =   Output(UInt(5.W))
    val s2_p0 =   Output(UInt(5.W))
    val rd_p0 =   Output(UInt(5.W))

    val s1_p1 =   Output(UInt(32.W))
    val s2_p1 =   Output(UInt(32.W))
    val rd_p1 =   Output(UInt(5.W))

    val s1_p2 =   Output(UInt(32.W))
    val s2_p2 =   Output(UInt(32.W))
    val rd_p2 =   Output(UInt(5.W))

    val debug =   Output(UInt(32.W))
  })

  val fetch = Module(new Fetch)
  val rf = Module(new RegisterFile(2))
  val alu = Module(new Alu)
  val hu = Module(new HazardUnit)

  // Connect fetch to the outside and pip0
  fetch.io.inst_in := io.inst_in
  io.pc := fetch.io.pc

  val rd_p0 = RegNext(fetch.io.dest, 0.U)
  val r1_p0 = RegNext(fetch.io.src1, 0.U)
  val r2_p0 = RegNext(fetch.io.src2, 0.U)
  val op_p0 = RegNext(fetch.io.alu_op, 0.U)
  val imm_p0 = RegNext(fetch.io.imm, 0.U)
  val imm_mux_p0 = RegNext(fetch.io.imm_mux, false.B)
  val mem_mux_p0 = RegNext(fetch.io.mem_mux, false.B)
  val flags_d_p0 = RegNext(fetch.io.flags_d, false.B)

  // Connect p0 to rf and pipeline signals
  rf.io.srcs(0) := r1_p0 
  rf.io.srcs(1) := r2_p0 
  val s2 = Wire(UInt(32.W))
  s2 := Mux(imm_mux_p0, imm_p0, rf.io.dests(1))

  val rd_p1 = RegNext(rd_p0, 0.U)
  val s1_p1 = RegNext(rf.io.dests(0), 0.U)
  val s2_p1 = RegNext(s2, 0.U)
  val op_p1 = RegNext(op_p0, 0.U)
  val mem_mux_p1 = RegNext(mem_mux_p0, false.B)
  val flags_d_p1 = RegNext(flags_d_p0, false.B)

  // Connect pip1 to the alu
  alu.io.src1 := s1_p1
  alu.io.src2 := s2_p1
  alu.io.op := op_p1

  val s1_p2 = RegNext(s1_p1, 0.U)
  val res_p2 = RegNext(alu.io.dest, 0.U)
  val rd_p2 = RegNext(rd_p1, 0.U)
  val mem_mux_p2 = RegNext(mem_mux_p1, false.B)
  
  // Connect pip2 to the memory io and the result to the rf
  // ...
  val w = Wire(UInt(32.W))
  w := Mux(mem_mux_p2, res_p2, 0.U)
  rf.io.write_data := w
  rf.io.write_reg := rd_p2

  // Connect the hazard unit to the pipeline
  hu.io.src1 := fetch.io.hu_src1
  hu.io.src2 := fetch.io.hu_src2
  hu.io.fd := fetch.io.hu_flags

  hu.io.rd_p0 := rd_p0
  hu.io.fd_p0 := flags_d_p0
  hu.io.rd_p1 := rd_p1
  hu.io.fd_p1 := flags_d_p1
  fetch.io.stall := hu.io.stall

  // Debug signals
  io.rf := rf.io.registers
  io.stall := hu.io.stall

  io.s1_p0 := r1_p0
  io.s2_p0 := r2_p0
  io.rd_p0 := rd_p0

  io.s1_p1 := s1_p1
  io.s2_p1 := s2_p1
  io.rd_p1 := rd_p1

  io.s1_p2 := s1_p2
  io.s2_p2 := res_p2
  io.rd_p2 := rd_p2

  io.debug := fetch.io.debug
  io.stall := hu.io.stall
}
