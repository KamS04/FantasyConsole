package ca.kam.metainstructions

class Instruction(
    val tag: String,
    val opcode: UByte,
    val type: InstructionBits.InstructionType,
    val mnemonic: String,
) {
    val size: UInt
        get() = InstructionBits.instructionSizes[type]!!

    override fun toString() = "0x${opcode.toString(16)} $mnemonic $tag"
}
