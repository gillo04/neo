package out_of_order 

import chisel3._
import chisel3.util._

class Control extends Bundle {
  val src1 =          UInt(5.W)
  val src2 =          UInt(5.W)
  val dest =          UInt(5.W)
  val imm =           UInt(32.W)
  val alu_op =        UInt(4.W)
  val imm_mux =       Bool()
  val mem_mux =       Bool()
  val mem_size =      UInt(2.W)
  val mem_sx =        Bool()      // Sign extend the value read from memory
  val mem_store =     Bool()
  val alu_d =         Bool()
  val dest_valid_0 =  Bool()
  val dest_valid_1 =  Bool()
}

class Fetch extends Module {
  val io = IO(new Bundle{
    // Icache
    val inst_in =     Input(UInt(32.W))
    val pc =          Output(UInt(32.W))

    // Hazard unit (dependencies)
    val stall =       Input(Bool())

    // Foreward signals
    val pip0 =        Output(new Control)

    // Jumping bypass
    val jmp_ready =   Input(Bool())       // The jmp_addr has been calculated
    val jmp_addr =    Input(UInt(32.W))
    val flags =       Input(UInt(3.W)) // Branch flags

    // Debug
    val debug = Output(UInt(32.W))
    val this_pc = Output(UInt(32.W))
    val this_inst = Output(UInt(32.W))
  })

  // instruction and program counter
  val pc = RegInit(0.U(32.W))
  val inst = RegInit(0.U(32.W))

  // Calculate next pc
  val jmp_mux = WireInit(false.B)
  val jmp_dest = WireInit(0.S(32.W)) // Calculated later
  val next_pc = Mux(jmp_mux, jmp_dest.asUInt, pc)
  val buff_pc = RegInit(0.U) // The pc of the instruction currently in inst
  val this_pc = RegInit(0.U) // The pc of the instruction currently in inst
  val jmp_state = RegInit(false.B) // 0: prepare jmp; 1: waiting for the jmp addr to be calculated 

  // Don't advance when stalling
  val internal_stall = WireInit(false.B)
  when (!io.stall & !internal_stall) {
    pc := next_pc + 4.U
    when (jmp_state === false.B | io.jmp_ready) {
      buff_pc := next_pc
      this_pc := buff_pc
      when (!jmp_mux) {
        inst := io.inst_in
      } .otherwise {
        inst := 0.U
      }
    }

    io.pc := next_pc
  } .otherwise {
    io.pc := buff_pc
  }


  // Instruction decoding
  
  // Dependencies
  val src1 = Wire(UInt(5.W))
  val src2 = Wire(UInt(5.W))
  src1 := 0.U
  src2 := 0.U

  io.pip0.src1 := src1
  io.pip0.src2 := src2
  io.pip0.dest := 0.U
  io.pip0.imm := 0.U
  io.pip0.alu_op := 0.U
  io.pip0.imm_mux := false.B
  io.pip0.mem_mux := false.B
  io.pip0.mem_size := 0.U
  io.pip0.mem_sx := false.B
  io.pip0.mem_store := false.B
  io.pip0.alu_d := false.B
  io.pip0.dest_valid_0 := false.B
  io.pip0.dest_valid_1 := false.B

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
  val b_imm1 = inst(11,8)
  val b_imm2 = inst(30,25)
  val b_imm3 = inst(7)
  val b_imm4 = inst(31)
  val b_src2 = inst(24,20)
  val b_src1 = inst(19,15)
  val b_funct3 = inst(14,12)

  // U-type
  val u_imm1 = inst(31,12)
  val u_dest = inst(11,7)

  // J-type
  val j_imm1 = inst(30,21)
  val j_imm2 = inst(20)
  val j_imm3 = inst(19,12)
  val j_imm4 = inst(31)
  val j_dest = inst(11,7)

  io.debug := 0.U
  switch (inst(6,0)) {
    is ("b0110111".U) {
      // U type
      // LUI
      io.pip0.dest_valid_0 := true.B
      io.pip0.dest := u_dest
      src1 := 0.U
      io.pip0.imm := Cat(u_imm1, 0.U(12.W))
      io.pip0.imm_mux := true.B
      io.pip0.mem_mux := true.B
    }
    is ("b0010111".U) {
      // U type
      // AUIPC
      io.pip0.dest_valid_0 := true.B
      io.pip0.dest := u_dest
      io.pip0.imm := Cat(u_imm1, 0.U(12.W)) + this_pc
      io.pip0.imm_mux := true.B
      io.pip0.mem_mux := true.B
    }
    is ("b1101111".U) {
      // J type
      // JAL
      io.pip0.dest_valid_0 := true.B
      jmp_mux := true.B
      jmp_dest := Cat(Seq(j_imm4, j_imm3, j_imm2, j_imm1, 0.U(1.W))).asSInt + this_pc.asSInt

      // Issue add rd, x0, new_pc
      io.pip0.imm := this_pc + 4.U
      src1 := 0.U
      io.pip0.dest := j_dest
      io.pip0.imm_mux := true.B
      io.pip0.mem_mux := true.B
      io.pip0.alu_op := 0.U
    }
    is ("b1100111".U) {
      // I type
      // JALR
      when (!io.stall) {   // When the dependency is solved
        when (jmp_state === false.B) {
          // Issue instruction to add rs1 to imm and stall the pipeline
          val sx = Wire(SInt(32.W))
          sx := i_imm1.asSInt
          io.pip0.imm := sx.asUInt
          io.pip0.src1 := i_src1
          io.pip0.dest := 0.U
          io.pip0.dest_valid_0 := true.B
          io.pip0.imm_mux := true.B
          io.pip0.alu_op := 0.U

          io.pip0.alu_d := true.B
          jmp_state := true.B
          internal_stall := true.B // Prevent fetching the next instruction
        } .elsewhen (io.jmp_ready === true.B) {
          // If jmp_ready, jmp_dest := jmp_addr
          jmp_dest := io.jmp_addr.asSInt
          jmp_mux := true.B

          // Issue link instruction
          io.pip0.dest_valid_0 := true.B
          io.pip0.imm := this_pc + 4.U
          io.pip0.src1 := 0.U
          io.pip0.dest := j_dest
          io.pip0.imm_mux := true.B
          io.pip0.mem_mux := true.B
          io.pip0.alu_op := 0.U
          jmp_state := false.B
        }
      }
    }
    is ("b1100011".U) {
      // B type
      jmp_dest := Cat(Seq(b_imm4, b_imm3, b_imm2, b_imm1, 0.U(1.W))).asSInt + this_pc.asSInt

      // BEQ
      // BNE
      // BLT
      // BGE
      // BLTU
      // BGEU
      when (!io.stall) {   // When the dependency is solved
        when (jmp_state === false.B) {
          // Issue add x0, r1, r2 so the alu can compare them
          io.pip0.src1 := b_src1
          io.pip0.src2 := b_src2
          io.pip0.dest := 0.U
          io.pip0.dest_valid_0 := true.B
          io.pip0.mem_mux := false.B
          io.pip0.alu_op := 0.U

          io.pip0.alu_d := true.B
          jmp_state := true.B
          internal_stall := true.B // Prevent fetching the next instruction
        } .elsewhen (io.jmp_ready === true.B) {
          // Decode flags
          val condition = Mux(
            b_funct3(2),
            Mux(b_funct3(1), io.flags(2), io.flags(1)),
            io.flags(0)
          )
          val take_branch = Mux(b_funct3(0), !condition, condition)

          // If jmp_ready and condition is met, make the jump
          when (take_branch) {
            jmp_mux := true.B
          }

          jmp_state := false.B
        }
      }
    }
    is ("b0000011".U) {
      // I type
      io.pip0.dest_valid_1 := true.B

      // LB
      // LH
      // LW
      // LBU
      // LHU
      src1 := i_src1
      io.pip0.dest := i_dest
      io.pip0.imm_mux := true.B
      io.pip0.mem_mux := false.B
      val tmp = Wire(SInt(32.W))
      tmp := i_imm1.asSInt
      io.pip0.imm := tmp.asUInt
      switch (s_funct3) {
        is ("b000".U) {
          // LB
          io.pip0.mem_size := 0.U;
        }
        is ("b001".U) {
          // LH
          io.pip0.mem_size := 1.U;
        }
        is ("b010".U) {
          // LW
          io.pip0.mem_size := 2.U;
        }
        is ("b100".U) {
          // LBU
          io.pip0.mem_size := 0.U;
          io.pip0.mem_sx := true.B;
        }
        is ("b101".U) {
          // LHU
          io.pip0.mem_size := 1.U;
          io.pip0.mem_sx := true.B;
        }
      }
    }
    is ("b0100011".U) {
      // S type

      src1 := s_src1
      src2 := s_src2
      io.pip0.dest := 0.U
      io.pip0.imm_mux := true.B
      io.pip0.mem_mux := false.B
      val tmp = Wire(SInt(32.W))
      tmp := Cat(s_imm1, s_imm2).asSInt
      io.pip0.imm := tmp.asUInt
      io.pip0.mem_store := true.B

      switch (s_funct3) {
        is ("b000".U) {
          // SB
          io.pip0.mem_size := 0.U;
        }
        is ("b001".U) {
          // SH
          io.pip0.mem_size := 1.U;
        }
        is ("b010".U) {
          // SW
          io.pip0.mem_size := 2.U;
        }
      }
    }
    is ("b0010011".U) {
      // I type
      io.pip0.dest_valid_0 := true.B
      src1 := i_src1
      io.pip0.dest := i_dest
      io.pip0.imm_mux := true.B
      io.pip0.mem_mux := true.B

      when (i_funct3 === "b001".U || i_funct3 === "b101".U) {
        // SLLI
        // SRLI
        // SRAI
        io.pip0.imm := i_imm1(4, 0)
        io.pip0.alu_op := Cat(i_imm1(10), i_funct3)
      } .otherwise {
        // ADDI
        // SLTI
        // SLTIU
        // XORI
        // ORI
        // ANDI
        val tmp = Wire(SInt(32.W))
        tmp := i_imm1.asSInt
        io.pip0.imm := tmp.asUInt
        io.pip0.alu_op := Cat(0.U(1.W), i_funct3)
      }
    }
    is ("b0110011".U) {
      // R type
      io.pip0.dest_valid_0 := true.B
      src1 := r_src1
      src2 := r_src2
      io.pip0.dest := r_dest
      io.pip0.mem_mux := true.B

      // ADD
      // SUB
      // SLL
      // SLT
      // SLTU
      // XOR
      // SRL
      // OR
      // AND
      io.pip0.alu_op := Cat(r_funct7(5), r_funct3)
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
  when (io.stall || (jmp_state === true.B && !io.jmp_ready)) {
    io.pip0.src1 := 0.U
    io.pip0.src2 := 0.U
    io.pip0.dest := 0.U
    io.pip0.imm := 0.U
    io.pip0.alu_op := 0.U
    io.pip0.imm_mux := false.B
    io.pip0.mem_mux := false.B
    io.pip0.mem_size := 0.U
    io.pip0.mem_sx := false.B
    io.pip0.mem_store := false.B
    io.pip0.alu_d := false.B
    io.pip0.dest_valid_0 := false.B
    io.pip0.dest_valid_1 := false.B
  }

  // Debug
  io.this_pc := this_pc
  io.this_inst := inst
}
