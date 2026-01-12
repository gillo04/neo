package in_order

import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage

class InOrder extends Module {
  val io = IO(new Bundle{
    // Icache
    val inst_in =     Input(UInt(32.W))
    val pc =          Output(UInt(32.W))

    // Dcache
    val addr =        Output(UInt(30.W))
    val read =        Input(UInt(32.W))
    val write =       Output(UInt(32.W))
    val write_mask =  Output(UInt(32.W))
    val write_en =    Output(Bool())

    // Debug
    val rf =          Output(Vec(32, UInt(32.W)))
    val stall =       Output(Bool())

    val s1_p0 =       Output(UInt(5.W))
    val s2_p0 =       Output(UInt(5.W))
    val rd_p0 =       Output(UInt(5.W))

    val s1_p1 =       Output(UInt(32.W))
    val s2_p1 =       Output(UInt(32.W))
    val rd_p1 =       Output(UInt(5.W))

    val s1_p2 =       Output(UInt(32.W))
    val s2_p2 =       Output(UInt(32.W))
    val rd_p2 =       Output(UInt(5.W))

    val this_pc =     Output(UInt(32.W))
    val this_inst =   Output(UInt(32.W))

    val debug =       Output(UInt(32.W))
  })

  val fetch = Module(new Fetch)
  val rf = Module(new RegisterFile(2))
  val alu = Module(new Alu)
  val mau = Module(new Mau)
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
  val mem_size_p0 = RegNext(fetch.io.mem_size, 0.U)
  val mem_sx_p0 = RegNext(fetch.io.mem_sx, false.B)
  val mem_store_p0 = RegNext(fetch.io.mem_store, false.B)
  val alu_d_p0 = RegNext(fetch.io.alu_d, false.B)

  // Connect p0 to rf and pipeline signals
  rf.io.srcs(0) := r1_p0 
  rf.io.srcs(1) := r2_p0 

  val rd_p1 = RegNext(rd_p0, 0.U)
  val s1_p1 = RegNext(rf.io.dests(0), 0.U)
  val s2_p1 = RegNext(rf.io.dests(1), 0.U)
  val imm_p1 = RegNext(imm_p0, 0.U)
  val op_p1 = RegNext(op_p0, 0.U)
  val imm_mux_p1 = RegNext(imm_mux_p0, false.B)
  val mem_mux_p1 = RegNext(mem_mux_p0, false.B)
  val mem_size_p1 = RegNext(mem_size_p0, 0.U)
  val mem_sx_p1 = RegNext(mem_sx_p0, false.B)
  val mem_store_p1 = RegNext(mem_store_p0, false.B)
  val alu_d_p1 = RegNext(alu_d_p0, false.B)

  // Connect pip1 to the alu
  alu.io.src1 := s1_p1
  alu.io.src2 := Mux(imm_mux_p1, imm_p1, s2_p1)
  alu.io.op := op_p1

  fetch.io.jmp_ready := alu_d_p1
  fetch.io.jmp_addr := alu.io.dest
  fetch.io.flags := alu.io.flags

  val s2_p2 = RegNext(s2_p1, 0.U)
  val res_p2 = RegNext(alu.io.dest, 0.U)
  val rd_p2 = RegNext(rd_p1, 0.U)
  val mem_mux_p2 = RegNext(mem_mux_p1, false.B)
  val mem_size_p2 = RegNext(mem_size_p1, 0.U)
  val mem_sx_p2 = RegNext(mem_sx_p1, false.B)
  val mem_store_p2 = RegNext(mem_store_p1, false.B)
  
  // Connect pip1 and pip2 to the memory io and the result to the rf
  mau.io.addr_p1 := alu.io.dest
  mau.io.addr_p2 := res_p2
  mau.io.write_p1 := s2_p1

  mau.io.read_m := io.read
  io.addr := mau.io.addr_m
  io.write := mau.io.write_m
  io.write_mask := mau.io.write_mask_m
  io.write_en := mem_store_p1

  mau.io.mem_size_p1 := mem_size_p1
  mau.io.mem_sx_p1 := mem_sx_p1 
  mau.io.mem_store_p1 := mem_store_p1

  mau.io.mem_size_p2 := mem_size_p2
  mau.io.mem_sx_p2 := mem_sx_p2 
  mau.io.mem_store_p2 := mem_store_p2

  rf.io.write_data := Mux(mem_mux_p2, res_p2, mau.io.read_p2)
  rf.io.write_reg := rd_p2

  // Connect the hazard unit to the pipeline
  hu.io.src1 := fetch.io.hu_src1
  hu.io.src2 := fetch.io.hu_src2

  hu.io.rd_p0 := rd_p0
  hu.io.alu_p0 := alu_d_p0
  hu.io.rd_p1 := rd_p1
  hu.io.alu_p1 := alu_d_p1
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

  io.s1_p2 := 0.U
  io.s2_p2 := 0.U 
  io.rd_p2 := rd_p2

  io.debug := Cat(alu_d_p1)
  io.stall := hu.io.stall
  io.this_pc := fetch.io.this_pc
  io.this_inst := fetch.io.this_inst
}

/*object InOrder extends App {
  ChiselStage.emitSystemVerilogFile(
    new InOrder,
    Array("--target-dir", "builds"),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info", "-default-layer-specialization=enable")
  )
}*/
