package ca.kam.fantasyconsole.machine

import ca.kam.fantasyconsole.cpu.CPU
import ca.kam.fantasyconsole.cpu.DefaultExecutor
import ca.kam.fantasyconsole.cpu.DefaultRegisters
import ca.kam.metainstructions.Instruction
import ca.kam.vmhardwarelibraries.TechDevice
import ca.kam.vmhardwarelibraries.cpu.Executor
import ca.kam.vmhardwarelibraries.cpu.RequiredRegisters
import ca.kam.vmhardwarelibraries.memory.MemoryDevice

class CPUBuilder {
    var memory: MemoryDevice? = null
    var registersNames: List<String> = DefaultRegisters.defaultRegisters
    var requiredRegisters: RequiredRegisters = DefaultRegisters
    var interruptVectorAddress: UShort = 0x0u
    var debugMode: Boolean = false
    var instructionsList: List<Instruction>? = null
    var executor: Executor = DefaultExecutor
    val techArray = mutableListOf<TechDevice>()
    var pauseAfterStep = false

    fun memory(memory: MemoryDevice) = apply { this.memory = memory }
    fun registersNames(registers: List<String>) = apply { this.registersNames = registers }
    fun requiredRegisters(registers: RequiredRegisters) = apply { this.requiredRegisters = registers }
    fun interrupt(vectorAddress: UShort) = apply { this.interruptVectorAddress = vectorAddress }
    fun debug(debugMode: Boolean) = apply { this.debugMode = debugMode }
    fun instructions(instrucs: List<Instruction>) = apply { this.instructionsList = instrucs }
    fun executor(executor: Executor) = apply { this.executor = executor }
    fun attach(device: TechDevice) = apply { this.techArray.add(device) }
    fun pauseAfterStep(step: Boolean) = apply { this.pauseAfterStep = step }

    fun build(): CPU {
        if (memory == null)
            throw Exception("No Memory was created")

        return CPU(
            memory!!,
            registersNames,
            requiredRegisters,
            interruptVectorAddress,
            debugMode,
            instructionsList,
            executor,
            techArray.toTypedArray(),
            pauseAfterStep
        )
    }
}
