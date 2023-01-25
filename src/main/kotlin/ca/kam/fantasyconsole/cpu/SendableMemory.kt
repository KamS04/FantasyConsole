package ca.kam.fantasyconsole.cpu

import ca.kam.vmhardwarelibraries.memory.Memory16
import ca.kam.vmhardwarelibraries.memory.Memory8
import ca.kam.vmhardwarelibraries.memory.MemoryDevice

@OptIn(ExperimentalUnsignedTypes::class)
fun createSendableMemory(memory: MemoryDevice): MemoryDevice {
    return object: MemoryDevice {
        override val name: String
            get() = memory.name

        override val bit8: Memory8
            get() = memory.bit8

        override val bit16: Memory16
            get() = memory.bit16

        override fun load(data: UByteArray, startAddress: UShort) = memory.load(data, startAddress)

        override fun slice(fAddress: UShort, tAddress: UShort) = memory.slice(fAddress, tAddress)
    }
}
