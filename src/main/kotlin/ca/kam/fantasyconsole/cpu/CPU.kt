package ca.kam.fantasyconsole.cpu

import ca.kam.fantasyconsole.*
import ca.kam.metainstructions.Instruction
import ca.kam.metainstructions.InstructionBits
import ca.kam.metainstructions.InstructionBits.createOpcodeGetter
import ca.kam.metainstructions.InstructionBits.readInstructionsCSV
import ca.kam.vmhardwarelibraries.TechDevice
import ca.kam.vmhardwarelibraries.cpu.CPUInterface
import ca.kam.vmhardwarelibraries.cpu.Executor
import ca.kam.vmhardwarelibraries.cpu.RequiredRegisters
import ca.kam.vmhardwarelibraries.memory.*

@OptIn(ExperimentalUnsignedTypes::class)
class CPU(
    val directMemory: MemoryDevice,
    val registersNames: List<String>,
    override val Registers: RequiredRegisters,
    val interruptVectorAddress: UShort = 0x1000u,
    val debugMode: Boolean = false,
    val instructionsList: List<Instruction>? = null,
    val executor: Executor = DefaultExecutor,
    val attachedDevices: Array<TechDevice> = emptyArray(),
    pauseAfterStep: Boolean
): CPUInterface {
    private var _pauseAfterStep = pauseAfterStep

    val pauseAfterStep
        get() = _pauseAfterStep

    override val memory: MemoryDevice = object: MemoryDevice {
        override val name: String = "CPU Memory"
        override fun load(data: UByteArray, startAddress: UShort) {
            directMemory.load(data, startAddress)
        }

        override fun slice(fAddress: UShort, tAddress: UShort): List<UByte> = directMemory.slice(fAddress, tAddress)

        override val bit16: Memory16 = object: Memory16 {
            override fun getUByte(address: UShort): UShort {
                return if (memMode == MemModes.RELATIVE) {
                    directMemory.bit16[(address + cMemSpace).toUShort()]
                } else {
                    directMemory.bit16[address]
                }
            }

            override fun setUByte(address: UShort, value: UShort): Boolean {
                return if (memMode == MemModes.RELATIVE) {
                    directMemory.bit16.setUByte((address + cMemSpace).toUShort(), value)
                } else {
                    directMemory.bit16.setUByte(address, value)
                }
            }
        }

        override val bit8: Memory8 = object: Memory8 {
            override fun getUByte(address: UShort): UByte {
                return if (memMode == MemModes.RELATIVE) {
                    directMemory.bit8[(address + cMemSpace).toUShort()]
                } else {
                    directMemory.bit8[address]
                }
            }

            override fun setUByte(address: UShort, value: UByte): Boolean {
                return if (memMode == MemModes.RELATIVE) {
                    directMemory.bit8.setUByte((address + cMemSpace).toUShort(), value)
                } else {
                    directMemory.bit8.setUByte(address, value)
                }
            }
        }
    }

    // All of these are RAV addresses
    override fun moveMemoryBlock(from: UShort, to: UShort, length: UShort) {
        directMemory.load(directMemory.slice(from, (from + length).s).toUByteArray(), to)
    }

    override var memMode: UByte = 1u
    override var cMemSpace: UShort = 0u

    private val mInstructionList: List<Instruction> by lazy {
        instructionsList ?: readInstructionsCSV( "/instructions.csv" )
    }

    val registersMap = registersNames
        .mapIndexed { index, s -> s to index }
        .toMap()

    override val registers = RAM(registersNames.size * 2, "CPU-Registers")

    var isInterruptHandler: Boolean = false
        get() = field
        set(value) {
            field = value
        }

//    var isInterruptHandler = false
    var stackFrameSize = 0u

    val debugStr: String
        get() = registersNames.mapIndexed { index, name ->
            "${ if (index > 0 && index % 4 == 0) "\n" else "" }$name:  ${getRegister(name).hexString()}    "
        }.joinToString("") + (if (memMode == MemModes.RELATIVE) " MM: Rel to ${cMemSpace.hexString()}" else " MM: abs")

    val opcodeGetter by lazy {
        createOpcodeGetter(mInstructionList)
    }

    var stackPosition: UShort

    init {
        val requiredRegisters = listOf(
            Registers.IP,
            Registers.ACU,
            Registers.SP,
            Registers.FP,
            Registers.IM,
        )

        if (!registersNames.containsAll(requiredRegisters)) {
            throw Exception("Required Registers are not all included in the registers list")
        }

        val startAddress = directMemory.bit16[interruptVectorAddress]
        setRegister(Registers.IP, startAddress)

        if (debugMode) {
            println("Starting at Instruction Address ${startAddress.hexString()}")
        }

        // Used to setup MemMode our selves, now it will be done via an actual interrupt
        // setupMemMode(MemModes.RELATIVE)

        setRegister(Registers.IM, 0xffffu)
        stackPosition = directMemory.bit16[(interruptVectorAddress + 2u).s]
        setRegister(Registers.SP, stackPosition)
        setRegister(Registers.FP, stackPosition)

        isInterruptHandler = true
        handleInterrupt(0u)
        isInterruptHandler = false // but set not in interrupt
    }

    // region Register Helpers
    override fun registerIndex(register: String): UShort {
        if (!registersNames.contains(register)) {
            throw Exception("Register does not exist")
        }

        return (registersMap[register]!! * 2).us
    }

    override fun regIdxToName(rIdx: UShort): String {
        return registersNames[(rIdx / 2u).toInt()]
    }

    override fun getRegister(register: String) = getRegister(registerIndex(register))

    override fun getRegister(registerIndex: UShort) = registers.bit16[registerIndex]

    override fun setRegister(register: String, value: UInt) = setRegister(register, value.s)

    override fun setRegister(register: String, value: UShort) = setRegister(registerIndex(register), value)

    override fun setRegister(registerIndex: UShort, value: UShort): Boolean {
        registers.bit16[registerIndex] = value
        return true
    }

    override fun getRealAddress(address: UShort): UShort {
        return when (memMode) {
            MemModes.ABSOLUTE -> {
                address
            }
            MemModes.RELATIVE -> {
                (cMemSpace + address).s
            }
            else -> throw Exception("Unknown memory mode")
        }
    }

    // endregion

    override fun signal(id: UByte) {
        attachedDevices[id.toInt()].signal()
    }

    // region Debug

    override fun debug() {
        println(debugStr)
    }

    fun getMemoryAt(startAddress: UShort, n: Int = 8): String = "${startAddress.hexString()}: " +
            (startAddress.toUInt() until startAddress + n.toUInt())
                .map { address ->
                    directMemory.bit8[address.toUShort()].byteString()
                }.joinToString(" ")

    fun viewMemoryAt(address: UShort, n: Int = 8) {
        println(getMemoryAt(address, n))
    }

    // endregion

    // region Fetch Helpers

    override fun fetch(noIncrement: Boolean): UByte {
        var iAddress = getRegister(Registers.IP)
        val value = directMemory.bit8[iAddress]
        if (!noIncrement) {
            setRegister(Registers.IP, ++iAddress)
        }
        return value
    }

    override fun fetch16(noIncrement: Boolean): UShort {
        val iAddress = getRegister(Registers.IP)
        val value = directMemory.bit16[iAddress]
        if (!noIncrement) {
            setRegister(Registers.IP, (iAddress + 2u).s )
        }
        return value
    }

    override fun fetchRegisterIndex(noIncrement: Boolean): UShort = (fetch(noIncrement) * 2u).s

    // endregion

    // region Subroutines

    // Old system the SP register contained the position to which the next
    // stack element should be written to
    // THIS IS DUMB, the SP register will contain the register where the previous
    // stack element was written to
    // TODO hopefully this doesn't break anything
    override fun push(value: UShort) {
        val spAddress = getRegister(Registers.SP)
        val nextAddress = (spAddress - 2u).s
        setRegister(Registers.SP, nextAddress)
        stackFrameSize += 2u
        // the stack used to reference the RAV address of the STACK_TOP
        // We're changing it to be a relative address
        // directMemory.bit16[nextAddress] = value
        memory.bit16[nextAddress] = value
    }

    override fun pop(): UShort {
        val spAddress = getRegister(Registers.SP)
        if (spAddress == stackPosition) {
            throw Exception("Popping Stack when empty")
        }
        val prevSpAddress = (spAddress + 2u).s
        setRegister(Registers.SP, prevSpAddress)
        stackFrameSize -= 2u
        // the stack used to reference the RAV address of the STACK_TOP
        // We're changing it to be a relative address
        // directMemory.bit16[nextAddress] = value
        // return directMemory.bit16[spAddress]
        return memory.bit16[spAddress]
    }


//    override fun push(value: UShort) {
//        val spAddress = getRegister(Registers.SP)
//        directMemory.bit16[spAddress] = value
//        stackFrameSize += 2u
//        setRegister(Registers.SP, (spAddress - 2u).s)
//    }

//    override fun pop(): UShort {
//        val spAddress = getRegister(Registers.SP)
//        if (spAddress == stackPosition) {
//            throw Exception("Popping Stack when empty")
//        }
//        val nextSpAddress = (spAddress + 2u).s
//        setRegister(Registers.SP, nextSpAddress)
//        stackFrameSize -= 2u
//        return directMemory.bit16[nextSpAddress]
//    }

    override fun pushState() {
        registersNames.forEach {
            if (it != Registers.ACU)
                push(getRegister(it))
        }
        push(stackFrameSize.s)

        setRegister(Registers.FP, getRegister(Registers.SP))
        stackFrameSize = 0u
    }

    override fun popState() {
        val framePointerAddress = getRegister(Registers.FP)
        setRegister(Registers.SP, framePointerAddress)

        stackFrameSize = pop().toUInt()
        val cStackFrameSize = stackFrameSize

        registersNames.reversed().forEach {
            if (it != Registers.ACU)
                setRegister(it, pop())
        }

        for (i in 0.us until pop()) {
            pop()
        }

        setRegister(Registers.FP, (framePointerAddress + stackFrameSize).s )
    }

    override fun handleInterrupt(value: UShort) {
//        val interruptBit = value % 0xfu
//
//        // If the interrupt is masked by the interrupt mask register
//        // then do not enter the interrupt handler
//        val isUnmasked = (1u).shl( interruptBit.toInt() ).and( getRegister(Registers.IM).toUInt() )
//        if (isUnmasked != 0u) {
//            return
//        }

        // better interrupt system
        if (value.and(getRegister(Registers.IM)) != value) {
            return
        }

        // Calculate where in the interrupt vector we'll look
        val addressPointer = interruptVectorAddress + (value * 2u)
        // Get the address from the interupt vector at that address
        val address = directMemory.bit16[addressPointer.s]

        // We only save state when not already in an interrupt
        if (!isInterruptHandler) {
            push(getRegister(Registers.IP))
        }

        isInterruptHandler = true

        // Jump to the interrupt handler
        setRegister(Registers.IP, address)
        setupMemMode(MemModes.RELATIVE)
    }

    override fun quickMemMode(nMode: UByte) {
        // The stack used to be an ABSOLUTE pointer
        // It will now be a RELATIVE
        // This means whenever a MemMode is setup it must be updated
        if (nMode != memMode) {
            val oldStackPointer = getRegister(Registers.SP)
            setRegister(
                Registers.SP,
                if (nMode == 0u.b) { // ABSOLUTE -> RELATIVE
                    oldStackPointer - cMemSpace
                } else { // RELATIVE -> ABSOLUTE
                    oldStackPointer + cMemSpace
                }
            )
            memMode = nMode
        }
    }

    override fun setupMemMode(nMode: UByte) {
        val newCMemSpace = if (directMemory is MemoryMapper) {
            directMemory.findDevice(getRegister(Registers.IP))?.start ?: 0u
        } else 0u
        var oldStackPointer = getRegister(Registers.SP)
        if (memMode == 0.ub) {
            oldStackPointer = (oldStackPointer + cMemSpace).s
        }
        setRegister(
            Registers.SP,
            if (nMode == 0.ub)
                (oldStackPointer - newCMemSpace).s
            else
                oldStackPointer
        )
        cMemSpace = newCMemSpace
        memMode = nMode
    }

    override fun jumpToAddress(address: UShort) {
        setRegister(Registers.IP, when (memMode) {
            MemModes.RELATIVE -> {
                (address + cMemSpace).s
            }
            MemModes.ABSOLUTE -> {
                address
            }
            else -> {
                throw Exception("Unknown Memory Mode")
            }
        })
    }

    override fun exitInterruptHandler() {
        setRegister(Registers.IP, pop())
        isInterruptHandler = false
        setupMemMode(MemModes.RELATIVE)
    }

    // endregion

    fun execute(instruction: UByte): Boolean {
        try {
            return executor.execute(this, instruction)
        } catch (e: Exception) {
            println("crash $e")
            println("crashing opcode ${instruction.hexString()}")
            println("ip: ${getRegister(Registers.IP).hexString()}")
            if (debugMode) println( opcodeGetter(instruction).tag )

            if (directMemory is MemoryMapper) {
                println("Crashing opcode in \"${directMemory.findDevice(getRegister(Registers.IP))!!.device.name}\" memory device")
            }

            throw e
        }
    }

    // region CPU Run Helpers

    fun step(): Boolean {
        if (debugMode) {
            debug()
            val address = getRegister(Registers.IP)
            if (directMemory is MapperOfMemory) {
                print((address - directMemory.findDevice(getRegister(Registers.IP))!!.start).hexString())
            } else {
                print(address.hexString())
            }
        }
        val instruction = fetch()
        if (debugMode) {
            try {
                val ins = opcodeGetter(instruction)
                val dS = getMemoryAt(getRegister(Registers.IP), (InstructionBits.instructionSizes[ins.type]!! - 1u).toInt())
                println(": ${ins.tag} \$$dS\n")
            } catch (e: Exception) {
//                println("Instruction Unknown with opcode: ${instruction.hexString()}")
            }
        }

        return execute(instruction)
    }

    fun run() {
//        var halt = false
        if (pauseAfterStep) {
            while (!step()) {
                readln()
            }
        } else {
            while (!step()) {
                continue
            }
        }
//        while (!halt) {
//            halt = step()
//        }
    }

    private fun startPausing() {
        _pauseAfterStep = true
    }
    private fun stopPausing() {
        _pauseAfterStep = false
    }


    // endregion

}