import chisel3._
import chisel3.util._

class Fetch extends Module {
  val io = IO(new Bundle{
    val src1 = Output(UInt(5.W))
    val src2 = Output(UInt(5.W))
    val dest = Output(UInt(5.W))
    val imm =  Output(UInt(64.W))

    val alu_op = Output(UInt(4.W))
  })

  val inst = Wire(UInt(32.W))

  io.src1 := 0.U
  io.src2 := 0.U
  io.dest := 0.U
  io.imm := 0.U
  io.alu_op := 0.U

  // R-type
  val r_funct7 = inst(31,25)
  val r_src2 = inst(24,20)
  val r_src1 = inst(19,15)
  val r_funct3 = inst(14,12)
  val r_dest = inst(11,7)
  
  // I-type
  val i_imm1 = inst(31,20)
  val i_src1 = inst(19,15)
  val i_funct3 = inst(14,12)
  val i_dest = inst(11,7)

  // S-type
  val s_imm1 = inst(31,25)
  val s_src2 = inst(24,20)
  val s_src1 = inst(19,15)
  val s_funct3 = inst(14,12)
  val s_imm2 = inst(11,7)

  // B-type
  val b_imm1 = inst(31,25)
  val b_src2 = inst(24,20)
  val b_src1 = inst(19,15)
  val b_funct3 = inst(14,12)
  val b_imm2 = inst(11,7)

  // U-type
  val u_imm1 = inst(31,12)
  val u_dest = inst(11,7)

  // J-type
  val j_imm1 = inst(31,12)
  val j_dest = inst(11,7)

  switch (inst(6,0)) {
    is ("b0110111".U) {
      // LUI
    }
    is ("b0010111".U) {
      // AUIPC
    }
    is ("b1101111".U) {
      // JAL
    }
    is ("b1100111".U) {
      // JALR
      // BEQ
      // BNE
      // BLT
      // BGE
      // BLTU
      // BGEU
    }
    is ("b0000011".U) {
      // LB
      // LH
      // LW
      // LBU
      // LHU
    }
    is ("b0100011".U) {
      // SB
      // SH
      // SW
    }
    is ("b0010011".U) {
      // ADDI
      // SLTI
      // SLTIU
      // XORI
      // ORI
      // ANDI
      // SLLI
      // SRLI
      // SRAI
    }
    is ("b0110011".U) {
      // ADD
      // SUB
      // SLL
      // SLT
      // SLTU
      // XOR
      // SRL
      // OR
      // AND
    }
    is ("b0001111".U) {
      // FENCE
      // FENCE.TSO
      // PAUSE
    }
    is ("b1110011".U) {
      // ECALL
      // EBREAK
    }
  }
}
