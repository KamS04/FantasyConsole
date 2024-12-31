package ca.kam.metainstructions

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.InputStream

object InstructionBits {
    enum class InstructionType {
        litReg,
        regLit,
        regLit8,
        regReg,
        regMem,
        memReg,
        litMem,
        lit8Mem,
        regPtrReg,
        litOffReg,
        noArgs,
        singleReg,
        singleLit,
        regRegPtr,
        singleMem,
        regPtr,
        singleLit8,
        regRegReg,
    }

    val instructionSizes = mapOf(
        InstructionType.litReg to 4u,
        InstructionType.regLit to 4u,
        InstructionType.regLit8 to 3u,
        InstructionType.regReg to 3u,
        InstructionType.regMem to 4u,
        InstructionType.memReg to 4u,
        InstructionType.litMem to 5u,
        InstructionType.lit8Mem to 4u,
        InstructionType.regPtrReg to 3u,
        InstructionType.regRegPtr to 3u,
        InstructionType.litOffReg to 5u,
        InstructionType.noArgs to 1u,
        InstructionType.singleReg to 2u,
        InstructionType.singleLit to 3u,
        InstructionType.singleMem to 3u,
        InstructionType.regPtr to 2u,
        InstructionType.singleLit8 to 2u,
        InstructionType.regRegReg to 4u,
    )

    fun readInstructionsCSV(path: String) = csvReader().open(path.asResourceStream() as InputStream) {
        try {
            readAllWithHeaderAsSequence().toList().map { row: Map<String, String> ->
                Instruction(
                    row["tag"]!!,
                    row["opcode"]!!.substring(2).toUByte(16),
                    InstructionType.valueOf(row["type"]!!),
                    row["mnemonic"]!!
                )
            }
        } catch (ex: Exception) {
            println("$path not a valid instruction csv file")
            throw ex
        }
    }

    fun createOpcodeGetter(instructions: List<Instruction>) = { opcode: UByte ->
        instructions.firstOrNull { it.opcode == opcode } ?: throw Exception("No instruction with opcode 0x${opcode.toString(16).padStart(2, '0')}")
    }

    fun createTagGetter(instructions: List<Instruction>) = { tag: String ->
        instructions.firstOrNull { it.tag == tag } ?: throw Exception("No instruction with tag $tag")
    }
}
