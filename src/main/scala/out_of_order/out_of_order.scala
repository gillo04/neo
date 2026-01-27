package out_of_order 

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

class OutOfOrder extends Module {
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
    val rf =          Output(Vec(32, new RfEntry(6)))
    val buffer =      Output(Vec(64, new RobEntry))
    val stall =       Output(Bool())

    val s1_p0 =       Output(UInt(5.W))
    val s2_p0 =       Output(UInt(5.W))
    val rd_p0 =       Output(UInt(5.W))
    val imm_p0 =      Output(UInt(32.W))
    val alu_d_p0 =    Output(Bool())
    val dest_valid_p0 = Output(Bool())

    val s1_p1 =       Output(UInt(32.W))
    val s2_p1 =       Output(UInt(32.W))
    val rd_p1 =       Output(UInt(5.W))
    val imm_p1 =      Output(UInt(32.W))
    val alu_d_p1 =    Output(Bool())
    val dest_valid_p1 = Output(Bool())

    val s1_p2 =       Output(UInt(32.W))
    val s2_p2 =       Output(UInt(32.W))
    val rd_p2 =       Output(UInt(5.W))
    val imm_p2 =      Output(UInt(32.W))
    val alu_d_p2 =    Output(Bool())
    val dest_valid_p2 = Output(Bool())

    val this_pc =     Output(UInt(32.W))
    val this_inst =   Output(UInt(32.W))

    val debug =       Output(UInt(32.W))
  })

  val fetch = Module(new Fetch)
  // val rf = Module(new RegisterFile(6, 2))
  val scheduler = Module(new Scheduler(6, 2, 2))
  val alu = Module(new Alu)
  val mau = Module(new Mau)
  val hu = Module(new HazardUnit)

  // Connect fetch to the outside and pip0
  fetch.io.inst_in := io.inst_in
  io.pc := fetch.io.pc

  val pip_fetch = RegInit((new Control).Lit(
    _.src1 -> 0.U,
    _.src2 -> 0.U,
    _.dest -> 0.U,
    _.imm -> 0.U,
    _.alu_op -> 0.U,
    _.imm_mux -> false.B,
    _.mem_mux -> false.B,
    _.mem_size -> 0.U,
    _.mem_sx -> false.B,
    _.mem_store -> false.B,
    _.alu_d -> false.B,
    _.dest_valid_0 -> false.B,
    _.dest_valid_1 -> false.B,
  ))
  
  // If stall, don't read from fetch
  when (!hu.io.stall) {
    pip_fetch := fetch.io.pip0
  }

  // Connect p0 to rf and pipeline signals
  scheduler.io.inst := pip_fetch
  scheduler.io.valid_inst := !hu.io.stall &
    (pip_fetch.dest_valid_0 | pip_fetch.dest_valid_1)

  val rd_p1 = RegInit(0.U)
  val nd_p1 = RegInit(0.U)
  val s1_p1 = RegInit(0.U)
  val s2_p1 = RegInit(0.U)
  val imm_p1 = RegInit(0.U)
  val op_p1 = RegInit(0.U)
  val imm_mux_p1 = RegInit(false.B)
  val mem_mux_p1 = RegInit(false.B)
  val mem_size_p1 = RegInit(0.U)
  val mem_sx_p1 = RegInit(false.B)
  val mem_store_p1 = RegInit(false.B)
  val alu_d_p1 = RegInit(false.B)
  val dest_valid_0_p1 = RegInit(false.B)
  val dest_valid_1_p1 = RegInit(false.B)

  // If stall, inject boubble
  when (!hu.io.stall) {
    rd_p1 := scheduler.io.issue.dest
    nd_p1 := scheduler.io.dest_addr
    s1_p1 := scheduler.io.vals(0)
    s2_p1 := scheduler.io.vals(1)
    imm_p1 := scheduler.io.issue.imm
    op_p1 := scheduler.io.issue.alu_op
    imm_mux_p1 := scheduler.io.issue.imm_mux
    mem_mux_p1 := scheduler.io.issue.mem_mux
    mem_size_p1 := scheduler.io.issue.mem_size
    mem_sx_p1 := scheduler.io.issue.mem_sx
    mem_store_p1 := scheduler.io.issue.mem_store
    alu_d_p1 := scheduler.io.issue.alu_d
    dest_valid_0_p1 := scheduler.io.issue.dest_valid_0
    dest_valid_1_p1 := scheduler.io.issue.dest_valid_1
  } .otherwise {
    rd_p1 := 0.U
    nd_p1 := 0.U
    s1_p1 := 0.U
    s2_p1 := 0.U
    imm_p1 := 0.U
    op_p1 := 0.U
    imm_mux_p1 := false.B
    mem_mux_p1 := false.B
    mem_size_p1 := 0.U
    mem_sx_p1 := false.B
    mem_store_p1 := false.B
    alu_d_p1 := false.B
    dest_valid_0_p1 := false.B
    dest_valid_1_p1 := false.B
  }

  // Connect pip1 to the alu
  alu.io.src1 := s1_p1
  alu.io.src2 := Mux(imm_mux_p1, imm_p1, s2_p1)
  alu.io.op := op_p1

  fetch.io.jmp_ready := alu_d_p1
  fetch.io.jmp_addr := alu.io.dest
  fetch.io.flags := alu.io.flags

  scheduler.io.pip_ports(0).value := alu.io.dest
  scheduler.io.pip_ports(0).dest := rd_p1
  scheduler.io.pip_ports(0).valid := dest_valid_0_p1
  scheduler.io.pip_ports(0).addr := nd_p1

  val s2_p2 = RegNext(s2_p1, 0.U)
  val res_p2 = RegNext(alu.io.dest, 0.U)
  val rd_p2 = RegNext(rd_p1, 0.U)
  val nd_p2 = RegNext(nd_p1, 0.U)
  val mem_mux_p2 = RegNext(mem_mux_p1, false.B)
  val mem_size_p2 = RegNext(mem_size_p1, 0.U)
  val mem_sx_p2 = RegNext(mem_sx_p1, false.B)
  val mem_store_p2 = RegNext(mem_store_p1, false.B)
  val dest_valid_1_p2 = RegNext(dest_valid_1_p1, false.B)
  
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

  scheduler.io.pip_ports(1).value := Mux(mem_mux_p2, res_p2, mau.io.read_p2)
  scheduler.io.pip_ports(1).dest := rd_p2
  scheduler.io.pip_ports(1).valid := dest_valid_1_p2
  scheduler.io.pip_ports(1).addr := nd_p2

  // Connect the hazard unit to the pipeline
  // TODO: remove the hazard unit, its no longer needed
  hu.io.alu_p0 := scheduler.io.issue.alu_d
  hu.io.scheduler := scheduler.io.stall
  fetch.io.stall := hu.io.stall_fetch

  // Debug signals
  io.rf := scheduler.io.registers
  io.buffer := scheduler.io.buffer
  io.stall := scheduler.io.stall

  io.s1_p0 := scheduler.io.issue.src1
  io.s2_p0 := scheduler.io.issue.src2
  io.rd_p0 := scheduler.io.issue.dest
  io.imm_p0 := scheduler.io.issue.imm
  io.alu_d_p0 := scheduler.io.issue.alu_d
  io.dest_valid_p0 := scheduler.io.issue.dest_valid_0

  io.s1_p1 := s1_p1
  io.s2_p1 := s2_p1
  io.rd_p1 := rd_p1
  io.imm_p1 := imm_p1
  io.alu_d_p1 := alu_d_p1
  io.dest_valid_p1 := dest_valid_0_p1

  io.s1_p2 := 0.U
  io.s2_p2 := 0.U 
  io.rd_p2 := rd_p2
  io.imm_p2 := 0.U
  io.alu_d_p2 := false.B 
  io.dest_valid_p2 := dest_valid_1_p2

  io.debug := alu_d_p1
  io.stall := hu.io.stall
  io.this_pc := fetch.io.this_pc
  io.this_inst := fetch.io.this_inst
}
