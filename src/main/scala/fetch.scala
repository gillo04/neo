import chisel3._
import chisel3.util._

class Fetch extends Module {
  val io = IO(new Bundle{
    // Icache
    val inst_in = Input(UInt(32.W))
    val pc =      Output(UInt(32.W))

    // Hazard unit
    val hu_src1 =    Output(UInt(5.W))
    val hu_src2 =    Output(UInt(5.W))
    val hu_flags =    Output(UInt(5.W))
    val stall =   Input(Bool())

    // Foreward
    val src1 =    Output(UInt(5.W))
    val src2 =    Output(UInt(5.W))
    val dest =    Output(UInt(5.W))
    val imm =     Output(UInt(32.W))
    val alu_op =  Output(UInt(4.W))
    val imm_mux = Output(Bool())
    val mem_mux = Output(Bool())
    val flags_d = Output(Bool())      // Depends on the flags

    // Debug
    val debug = Output(UInt(32.W))
  })

  // instruction and program counter
  val pc = RegInit(0.U(32.W))
  val inst = RegInit(0.U(32.W))

  // Calculate next pc
  val jmp_mux = Wire(Bool())
  jmp_mux := false.B
  val jmp_dest = Wire(SInt(32.W)) // Calculated later
  val next_pc = Wire(UInt(32.W))
  next_pc := Mux(jmp_mux, jmp_dest.asUInt, pc)

  // Don't advance when stalling
  when (!io.stall) {
    pc := next_pc + 4.U
    inst := io.inst_in
  }
  io.pc := next_pc

  // Instruction decoding
  val src1 = Wire(UInt(5.W))
  val src2 = Wire(UInt(5.W))
  src1 := 0.U
  src2 := 0.U
  io.hu_src1 := src1
  io.hu_src2 := src2
  io.hu_flags := false.B

  io.src1 := src1
  io.src2 := src2
  io.dest := 0.U
  io.imm := 0.U
  io.alu_op := 0.U
  io.imm_mux := false.B
  io.mem_mux := false.B
  io.flags_d := false.B

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
  val j_imm1 = inst(30,21)
  val j_imm2 = inst(20)
  val j_imm3 = inst(19,12)
  val j_imm4 = inst(31)
  val j_dest = inst(11,7)
  jmp_dest := Cat(Seq(j_imm4, j_imm3, j_imm2, j_imm1, 0.U(1.W))).asSInt + pc.asSInt - 4.S // ERROR Because pc already points to the next inst

  io.debug := 0.U
  switch (inst(6,0)) {
    is ("b0110111".U) {
      // U type
      // LUI
      io.dest := u_dest
      io.imm := Cat(u_imm1, 0.U(12.W))
      io.imm_mux := true.B
      io.mem_mux := true.B
    }
    is ("b0010111".U) {
      // U type
      // AUIPC
      io.dest := u_dest
      io.imm := Cat(u_imm1, 0.U(12.W)) + pc
      io.imm_mux := true.B
      io.mem_mux := true.B
    }
    is ("b1101111".U) {
      // J type
      // JAL
      jmp_mux := true.B

      // Issue add rd, x0, new_pc
      io.imm := jmp_dest.asUInt
      src1 := 0.U
      io.dest := j_dest
      io.imm_mux := true.B
      io.mem_mux := true.B
      io.alu_op := 0.U

      io.debug := 1.U
    }
    is ("b1100111".U) {
      // I type
      // JALR
      
      // B type
      // BEQ
      // BNE
      // BLT
      // BGE
      // BLTU
      // BGEU
    }
    is ("b0000011".U) {
      // I type
      // LB
      // LH
      // LW
      // LBU
      // LHU
    }
    is ("b0100011".U) {
      // S type
      // SB
      // SH
      // SW
    }
    is ("b0010011".U) {
      // I type
      src1 := i_src1
      io.dest := i_dest
      io.imm_mux := true.B
      io.mem_mux := true.B
      io.flags_d := true.B

      io.debug := 2.U

      when (i_funct3 === "b001".U || i_funct3 === "b101".U) {
        // SLLI
        // SRLI
        // SRAI
        io.imm := i_imm1(4, 0)
        io.alu_op := Cat(i_imm1(10), i_funct3)
      } .otherwise {
        // ADDI
        // SLTI
        // SLTIU
        // XORI
        // ORI
        // ANDI
        val tmp = Wire(SInt(32.W))
        tmp := i_imm1.asSInt
        io.imm := tmp.asUInt
        io.alu_op := Cat(0.U(1.W), i_funct3)
      }
    }
    is ("b0110011".U) {
      // R type
      src1 := r_src1
      src2 := r_src2
      io.dest := r_dest
      io.mem_mux := true.B
      io.flags_d := true.B

      io.debug := 3.U
      // ADD
      // SUB
      // SLL
      // SLT
      // SLTU
      // XOR
      // SRL
      // OR
      // AND
      io.alu_op := Cat(r_funct7(5), r_funct3)
    }
    is ("b0001111".U) {
      // I type
      // FENCE
      // FENCE.TSO
      // PAUSE
    }
    is ("b1110011".U) {
      // I type
      // ECALL
      // EBREAK
    }
  }

  // When stalling ensure to issue nops and to keep the HU clear
  when (io.stall) {
    io.dest := 0.U
    io.src1 := 0.U
    io.src2 := 0.U
    io.alu_op := 0.U
    io.imm_mux := false.B
    io.mem_mux := false.B
    io.flags_d := false.B
  }
}
