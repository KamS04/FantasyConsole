package ca.kam.fantasyconsole.cpu

import ca.kam.metainstructions.InstructionBits.createTagGetter
import ca.kam.metainstructions.InstructionBits.readInstructionsCSV

object InstructionsMapper {
    val opcodeGetter by lazy {
        val tagGetter = createTagGetter(
            readInstructionsCSV("/instructions.csv")
        )
        return@lazy { tag: String ->
            tagGetter(tag).opcode
        }
    }

    val LOG_REG by lazy { opcodeGetter("LOG_REG") }
    val LOG_REG_PTR by lazy { opcodeGetter("LOG_REG_PTR") }
    val LOG_MEM by lazy { opcodeGetter("LOG_MEM") }
    val MOV8_LIT_MEM by lazy { opcodeGetter("MOV8_LIT_MEM") }
    val MOV8_MEM_REG by lazy { opcodeGetter("MOV8_MEM_REG") }
    val MOVL_REG_MEM by lazy { opcodeGetter("MOVL_REG_MEM") }
    val MOVH_REG_MEM by lazy { opcodeGetter("MOVH_REG_MEM") }
    val MOV8_REG_PTR_REG by lazy { opcodeGetter("MOV8_REG_PTR_REG") }
    val MOV8_REG_REG_PTR by lazy { opcodeGetter("MOV8_REG_REG_PTR") }
    val INT_LIT by lazy { opcodeGetter("INT_LIT") }
    val INT_REG by lazy { opcodeGetter("INT_REG") }
    val RET_INT by lazy { opcodeGetter("RET_INT") }
    val MOV_LIT_REG by lazy { opcodeGetter("MOV_LIT_REG") }
    val MOV_REG_REG by lazy { opcodeGetter("MOV_REG_REG") }
    val MOV_REG_REG_PTR by lazy { opcodeGetter("MOV_REG_REG_PTR") }
    val MOV_REG_MEM by lazy { opcodeGetter("MOV_REG_MEM") }
    val MOV_MEM_REG by lazy { opcodeGetter("MOV_MEM_REG") }
    val MOV_LIT_MEM by lazy { opcodeGetter("MOV_LIT_MEM") }
    val MOV_REG_PTR_REG by lazy { opcodeGetter("MOV_REG_PTR_REG") }
    val MOV_LIT_OFF_REG by lazy { opcodeGetter("MOV_LIT_OFF_REG") }
    val MOV_BLOCK by lazy { opcodeGetter("MOV_BLOCK") }
    val ADD_REG_REG by lazy { opcodeGetter("ADD_REG_REG") }
    val ADD_LIT_REG by lazy { opcodeGetter("ADD_LIT_REG") }
    val SUB_LIT_REG by lazy { opcodeGetter("SUB_LIT_REG") }
    val SUB_REG_LIT by lazy { opcodeGetter("SUB_REG_LIT") }
    val SUB_REG_REG by lazy { opcodeGetter("SUB_REG_REG") }
    val INC_REG by lazy { opcodeGetter("INC_REG") }
    val DEC_REG by lazy { opcodeGetter("DEC_REG") }
    val MUL_LIT_REG by lazy { opcodeGetter("MUL_LIT_REG") }
    val MUL_REG_REG by lazy { opcodeGetter("MUL_REG_REG") }
    val LSF_REG_LIT by lazy { opcodeGetter("LSF_REG_LIT") }
    val LSF_REG_REG by lazy { opcodeGetter("LSF_REG_REG") }
    val RSF_REG_LIT by lazy { opcodeGetter("RSF_REG_LIT") }
    val RSF_REG_REG by lazy { opcodeGetter("RSF_REG_REG") }
    val AND_REG_LIT by lazy { opcodeGetter("AND_REG_LIT") }
    val AND_REG_REG by lazy { opcodeGetter("AND_REG_REG") }
    val OR_REG_LIT by lazy { opcodeGetter("OR_REG_LIT") }
    val OR_REG_REG by lazy { opcodeGetter("OR_REG_REG") }
    val XOR_REG_LIT by lazy { opcodeGetter("XOR_REG_LIT") }
    val XOR_REG_REG by lazy { opcodeGetter("XOR_REG_REG") }
    val NOT by lazy { opcodeGetter("NOT") }
    val JMP_REG by lazy { opcodeGetter("JMP_REG") }
    val JMP_LIT by lazy { opcodeGetter("JMP_LIT") }
    val JMP_NOT_EQ by lazy { opcodeGetter("JMP_NOT_EQ") }
    val JNE_REG by lazy { opcodeGetter("JNE_REG") }
    val JEQ_REG by lazy { opcodeGetter("JEQ_REG") }
    val JEQ_LIT by lazy { opcodeGetter("JEQ_LIT") }
    val JNE_LIT by lazy { opcodeGetter("JNE_LIT") }
    val JLT_REG by lazy { opcodeGetter("JLT_REG") }
    val JLT_LIT by lazy { opcodeGetter("JLT_LIT") }
    val JGT_REG by lazy { opcodeGetter("JGT_REG") }
    val JGT_LIT by lazy { opcodeGetter("JGT_LIT") }
    val JLE_REG by lazy { opcodeGetter("JLE_REG") }
    val JLE_LIT by lazy { opcodeGetter("JLE_LIT") }
    val JGE_REG by lazy { opcodeGetter("JGE_REG") }
    val JGE_LIT by lazy { opcodeGetter("JGE_LIT") }
    val PSH_LIT by lazy { opcodeGetter("PSH_LIT") }
    val PSH_REG by lazy { opcodeGetter("PSH_REG") }
    val POP by lazy { opcodeGetter("POP") }
    val CAL_LIT by lazy { opcodeGetter("CAL_LIT") }
    val CAL_REG by lazy { opcodeGetter("CAL_REG") }
    val RET by lazy { opcodeGetter("RET") }
    val HLT by lazy { opcodeGetter("HLT") }
    val BRK by lazy { opcodeGetter("BRK") }
    val NOP by lazy { opcodeGetter("NOP") }
    val MEM_MODE_SET by lazy { opcodeGetter("MEM_MODE_SET") }
    val SEND_SIG by lazy { opcodeGetter("SEND_SIG") }
    val REAL_REG_PTR by lazy { opcodeGetter("REAL_REG_PTR") }
    val REAL_MEM by lazy { opcodeGetter("REAL_MEM") }
}