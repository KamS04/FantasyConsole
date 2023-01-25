package ca.kam.fantasyconsole.cpu

import ca.kam.fantasyconsole.*
import ca.kam.vmhardwarelibraries.cpu.CPUInterface
import ca.kam.vmhardwarelibraries.cpu.Executor
import ca.kam.vmhardwarelibraries.cpu.UnknownInstructionException


object DefaultExecutor: Executor {

    override fun execute(cpu: CPUInterface, opcode: UByte): Boolean {
        cpu.run {
            when (opcode) {
                //region Log/Debugger
                InstructionsMapper.LOG_REG -> {
                    val registerIndex = fetchRegisterIndex()
                    val value = registers.bit16[registerIndex]
                    println("LOG: $registerIndex, $value, ${value.hexString()}")
                }

                InstructionsMapper.LOG_REG_PTR -> {
                    val registerIndex = fetchRegisterIndex()
                    val address = registers.bit16[registerIndex]
                    val value = memory.bit16[address]
                    println("LOG: $address: $value | HEX - ${address.hexString()}: ${value.hexString()}")
                }

                InstructionsMapper.LOG_MEM -> {
                    val address = fetch16()
                    val value = memory.bit16[address]
                    println("LOG: $address: $value | HEX - ${address.hexString()}: ${value.hexString()}")
                }

                InstructionsMapper.BRK -> {
                    readln()
                    return false
                }
                //endregion

                //region Subroutines
                InstructionsMapper.PSH_LIT -> {
                    val value = fetch16()
                    push(value)
                }

                InstructionsMapper.PSH_REG -> {
                    val reg = fetchRegisterIndex()
                    push(registers.bit16[reg])
                }

                InstructionsMapper.POP -> {
                    val reg = fetchRegisterIndex()
                    val value = pop()
                    registers.bit16[reg] = value
                }

                InstructionsMapper.CAL_LIT -> {
                    val address = fetch16()
                    pushState()
                    setRegister(Registers.IP, getRealAddress(address))
                }

                InstructionsMapper.CAL_REG -> {
                    val reg = fetchRegisterIndex()
                    val address = registers.bit16[reg]
                    pushState()
                    setRegister(Registers.IP, getRealAddress(address))
                }

                InstructionsMapper.RET -> {
                    popState()
                }

                InstructionsMapper.RET_INT -> {
//                    setRegister(Registers.IP, pop())
                    exitInterruptHandler()
                }

                InstructionsMapper.INT_LIT -> {
                    val interruptValue = fetch16()
                    handleInterrupt(interruptValue)
                }

                InstructionsMapper.INT_REG -> {
                    val registerIndex = fetchRegisterIndex()
                    val interruptValue = registers.bit16[registerIndex]
                    handleInterrupt(interruptValue)
                }
                //endregion

                //region Mov
                InstructionsMapper.MOV8_LIT_MEM -> {
                    val lit = fetch()
                    val address = fetch16()
                    memory.bit8[address] = lit
                }

                InstructionsMapper.MOV8_MEM_REG -> {
                    val address = fetch16()
                    val registerTo = fetchRegisterIndex()
                    val value = memory.bit8[address]
                    registers.bit16[registerTo] = value.s
                }

                InstructionsMapper.MOVL_REG_MEM -> {
                    val registerFrom = fetchRegisterIndex()
                    val address = fetch16()
                    val value = registers.bit16[registerFrom].and(0xffu).b
                    memory.bit8[address] = value
                }

                InstructionsMapper.MOVH_REG_MEM -> {
                    val registerFrom = fetchRegisterIndex()
                    val address = fetch16()
                    val value = registers.bit8[registerFrom]
                    memory.bit8[address] = value
                }

                InstructionsMapper.MOV8_REG_PTR_REG -> {
                    val r1 = fetchRegisterIndex()
                    val r2 = fetchRegisterIndex()
                    val ptr = registers.bit16[r1]
                    val value = memory.bit8[ptr]
                    registers.bit16[r2] = value.s
                }

                InstructionsMapper.MOV8_REG_REG_PTR -> {
                    val r1 = fetchRegisterIndex()
                    val r2 = fetchRegisterIndex()
                    val value = registers.bit16[r1].and(0xffu).b
                    val addr = registers.bit16[r2]
                    memory.bit8[addr] = value
                }

                InstructionsMapper.MOV_LIT_REG -> {
                    val literal = fetch16()
                    val register = fetchRegisterIndex()
                    registers.bit16[register] = literal
                }

                InstructionsMapper.MOV_REG_REG -> {
                    val registerFrom = fetchRegisterIndex()
                    val registerTo = fetchRegisterIndex()
                    val value = registers.bit16[registerFrom]
                    registers.bit16[registerTo] = value
                }

                InstructionsMapper.MOV_REG_MEM -> {
                    val registerFrom = fetchRegisterIndex()
                    val address = fetch16()
                    val value = registers.bit16[registerFrom]
                    memory.bit16[address] = value
                }

                InstructionsMapper.MOV_MEM_REG -> {
                    val address = fetch16()
                    val registerTo = fetchRegisterIndex()
                    val value = memory.bit16[address]
                    registers.bit16[registerTo] = value
                }

                InstructionsMapper.MOV_LIT_MEM -> {
                    val value = fetch16()
                    val address = fetch16()
                    memory.bit16[address] = value
                }

                InstructionsMapper.MOV_REG_PTR_REG -> {
                    val r1 = fetchRegisterIndex()
                    val r2 = fetchRegisterIndex()
                    val addr = registers.bit16[r1]
                    val value = memory.bit16[addr]
                    registers.bit16[r2] = value
                }

                InstructionsMapper.MOV_REG_REG_PTR -> {
                    val r1 = fetchRegisterIndex()
                    val r2 = fetchRegisterIndex()
                    val value = registers.bit16[r1]
                    val addr = registers.bit16[r2]
                    memory.bit16[addr] = value
                }

                InstructionsMapper.MOV_LIT_OFF_REG -> {
                    val baseAddress = fetch16()
                    val registerFrom = fetchRegisterIndex()
                    val registerTo = fetchRegisterIndex()
                    val offset = registers.bit16[registerFrom]

                    val value = memory.bit16[(baseAddress + offset).s]
                    registers.bit16[registerTo] = value
                }
                //endregion

                //region Math Operations
                InstructionsMapper.ADD_REG_REG -> {
                    val r1 = fetchRegisterIndex()
                    val r2 = fetchRegisterIndex()
                    val regVal1 = registers.bit16[r1]
                    val regVal2 = registers.bit16[r2]
                    setRegister(Registers.ACU, regVal1 + regVal2)
                }

                InstructionsMapper.ADD_LIT_REG -> {
                    val literal = fetch16()
                    val reg = fetchRegisterIndex()
                    val regVal = registers.bit16[reg]
                    setRegister(Registers.ACU, literal + regVal)
                }

                InstructionsMapper.SUB_LIT_REG -> {
                    val reg = fetchRegisterIndex()
                    val literal = fetch16()
                    val regVal = registers.bit16[reg]
                    setRegister(Registers.ACU, regVal - literal)
                }

                InstructionsMapper.SUB_REG_LIT -> {
                    val reg = fetchRegisterIndex()
                    val literal = fetch16()
                    val regVal = registers.bit16[reg]
                    setRegister(Registers.ACU, literal - regVal)
                }

                InstructionsMapper.SUB_REG_REG -> {
                    val r1 = fetchRegisterIndex()
                    val r2 = fetchRegisterIndex()
                    val regVal1 = registers.bit16[r1]
                    val regVal2 = registers.bit16[r2]
                    setRegister(Registers.ACU, regVal2 + regVal1)
                }

                InstructionsMapper.MUL_LIT_REG -> {
                    val literal = fetch16()
                    val reg = fetchRegisterIndex()
                    val regVal = registers.bit16[reg]
                    setRegister(Registers.ACU, literal * regVal)
                }

                InstructionsMapper.MUL_REG_REG -> {
                    val r1 = fetchRegisterIndex()
                    val r2 = fetchRegisterIndex()
                    val regVal1 = registers.bit16[r1]
                    val regVal2 = registers.bit16[r2]
                    setRegister(Registers.ACU, regVal1 * regVal2)
                }

                InstructionsMapper.INC_REG -> {
                    val reg = fetchRegisterIndex()
                    var oldVal = registers.bit16[reg]
                    registers.bit16[reg] = ++oldVal
                }

                InstructionsMapper.DEC_REG -> {
                    val reg = fetchRegisterIndex()
                    var oldVal = registers.bit16[reg]
                    registers.bit16[reg] = --oldVal
                }
                //endregion

                //region Bitwise Operations
                InstructionsMapper.LSF_REG_REG -> {
                    val r1 = fetchRegisterIndex()
                    val r2 = fetchRegisterIndex()
                    val regVal = registers.bit16[r1]
                    val shiftBy = registers.bit16[r2]
                    registers.bit16[r1] = regVal shl shiftBy
                }

                InstructionsMapper.LSF_REG_LIT -> {
                    val reg = fetchRegisterIndex()
                    val literal = fetch()
                    val regVal = registers.bit16[reg]
                    registers.bit16[reg] = regVal shl literal
                }

                InstructionsMapper.RSF_REG_REG -> {
                    val r1 = fetchRegisterIndex()
                    val r2 = fetchRegisterIndex()
                    val regVal = registers.bit16[r1]
                    val shiftBy = registers.bit16[r2]
                    registers.bit16[r1] = regVal shr shiftBy
                }

                InstructionsMapper.RSF_REG_LIT -> {
                    val reg = fetchRegisterIndex()
                    val literal = fetch()
                    val regVal = registers.bit16[reg]
                    registers.bit16[reg] = regVal shr literal
                }

                InstructionsMapper.AND_REG_LIT -> {
                    val reg = fetchRegisterIndex()
                    val literal = fetch16()
                    val regVal = registers.bit16[reg]
                    setRegister(Registers.ACU, regVal.and(literal))
                }

                InstructionsMapper.AND_REG_REG -> {
                    val r1 = fetchRegisterIndex()
                    val r2 = fetchRegisterIndex()
                    val regVal1 = registers.bit16[r1]
                    val regVal2 = registers.bit16[r2]
                    setRegister(Registers.ACU, regVal1.and(regVal2))
                }

                InstructionsMapper.OR_REG_LIT -> {
                    val reg = fetchRegisterIndex()
                    val literal = fetch16()
                    val regVal = registers.bit16[reg]
                    setRegister(Registers.ACU, regVal.or(literal))
                }

                InstructionsMapper.OR_REG_REG -> {
                    val r1 = fetchRegisterIndex()
                    val r2 = fetchRegisterIndex()
                    val regVal1 = registers.bit16[r1]
                    val regVal2 = registers.bit16[r2]
                    setRegister(Registers.ACU, regVal1.or(regVal2))
                }

                InstructionsMapper.XOR_REG_LIT -> {
                    val reg = fetchRegisterIndex()
                    val literal = fetch16()
                    val regVal = registers.bit16[reg]
                    setRegister(Registers.ACU, regVal.xor(literal))
                }

                InstructionsMapper.XOR_REG_REG -> {
                    val r1 = fetchRegisterIndex()
                    val r2 = fetchRegisterIndex()
                    val regVal1 = registers.bit16[r1]
                    val regVal2 = registers.bit16[r2]
                    setRegister(Registers.ACU, regVal1.xor(regVal2))
                }

                InstructionsMapper.NOT -> {
                    val reg = fetchRegisterIndex()
                    val regVal = registers.bit16[reg]
                    setRegister(Registers.ACU, regVal.inv())
                }
                //endregion

                //region Conditionals
                InstructionsMapper.JMP_NOT_EQ -> {
                    val value = fetch16()
                    val address = fetch16()
                    if (value != getRegister(Registers.ACU))
                        jumpToAddress(address)
//                        setRegister(Registers.IP, address)
                    return false
                }

                InstructionsMapper.JNE_REG -> {
                    val reg = fetchRegisterIndex()
                    val value = registers.bit16[reg]
                    val address = fetch16()

                    if (value != getRegister(Registers.ACU))
                        jumpToAddress(address)
//                        setRegister(Registers.IP, address)
                    return false
                }

                InstructionsMapper.JEQ_LIT -> {
                    val value = fetch16()
                    val address = fetch16()

                    if (value == getRegister(Registers.ACU))
                        jumpToAddress(address)
//                        setRegister(Registers.IP, address)
                    return false
                }

                InstructionsMapper.JEQ_REG -> {
                    val reg = fetchRegisterIndex()
                    val address = fetch16()
                    val regVal = registers.bit16[reg]

                    if (regVal == getRegister(Registers.ACU))
                        jumpToAddress(address)
//                        setRegister(Registers.IP, address)
                    return false
                }

                InstructionsMapper.JLT_LIT -> {
                    val value = fetch16()
                    val address = fetch16()

                    if (value < getRegister(Registers.ACU))
                        jumpToAddress(address)
//                        setRegister(Registers.IP, address)
                    return false
                }

                InstructionsMapper.JLT_REG -> {
                    val reg = fetchRegisterIndex()
                    val address = fetch16()
                    val regVal = registers.bit16[reg]

                    if (regVal < getRegister(Registers.ACU))
                        jumpToAddress(address)
//                    setRegister(Registers.IP, address)
                    return false
                }

                InstructionsMapper.JGT_LIT -> {
                    val value = fetch16()
                    val address = fetch16()

                    if (value > getRegister(Registers.ACU))
                        jumpToAddress(address)
//                        setRegister(Registers.IP, address)
                    return false
                }

                InstructionsMapper.JGT_REG -> {
                    val reg = fetchRegisterIndex()
                    val address = fetch16()
                    val regVal = registers.bit16[reg]

                    if (regVal > getRegister(Registers.ACU))
                        jumpToAddress(address)
//                        setRegister(Registers.IP, address)
                    return false
                }

                InstructionsMapper.JLE_LIT -> {
                    val value = fetch16()
                    val address = fetch16()

                    if (value <= getRegister(Registers.ACU))
                        jumpToAddress(address)
//                        setRegister(Registers.IP, address)
                    return false
                }

                InstructionsMapper.JLE_REG -> {
                    val reg = fetchRegisterIndex()
                    val address = fetch16()
                    val regVal = registers.bit16[reg]

                    if (regVal <= getRegister(Registers.ACU))
                        jumpToAddress(address)
//                      setRegister(Registers.IP, address)
                    return false
                }

                InstructionsMapper.JGE_LIT -> {
                    val value = fetch16()
                    val address = fetch16()

                    if (value >= getRegister(Registers.ACU))
                        jumpToAddress(address)
//                        setRegister(Registers.IP, address)
                    return false
                }

                InstructionsMapper.JGE_REG -> {
                    val reg = fetchRegisterIndex()
                    val address = fetch16()
                    val regVal = registers.bit16[reg]

                    if (regVal >= getRegister(Registers.ACU))
                        jumpToAddress(address)
//                        setRegister(Registers.IP, address)
                    return false
                }
                //endregion

                InstructionsMapper.HLT -> {
                    return true
                }

                InstructionsMapper.MEM_MODE_SET -> {
                    this.memMode = fetch()
                }

                InstructionsMapper.SEND_SIG -> {
                    val r1 = fetchRegisterIndex()
                    val id = registers.bit16[r1].b
                    signal(id)
                    return false
                }

                InstructionsMapper.REAL_REG_PTR -> {
                    val regIdx = fetchRegisterIndex()
                    val address = registers.bit16[regIdx]
                    val toIdx = fetchRegisterIndex()
                    setRegister(
                        toIdx,
                        getRealAddress(address)
                    )
                }

                else -> throw UnknownInstructionException(opcode.hexString())
            }
        }

        return false
    }
}
