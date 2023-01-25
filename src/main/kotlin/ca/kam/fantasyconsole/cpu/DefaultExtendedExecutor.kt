package ca.kam.fantasyconsole.cpu

import ca.kam.vmhardwarelibraries.cpu.CPUInterface
import ca.kam.vmhardwarelibraries.cpu.Executor
import ca.kam.vmhardwarelibraries.cpu.ExtendedExecutor
import ca.kam.vmhardwarelibraries.cpu.UnknownInstructionException

class DefaultExtendedExecutor(
    val defaultExecutor: Executor,
    val extendor: ExtendedExecutor
): Executor {
    override fun execute(cpu: CPUInterface, opcode: UByte): Boolean = try {
        defaultExecutor.execute(cpu, opcode)
    } catch (e: UnknownInstructionException) {
        extendor.extend(cpu, opcode)
    }
}
