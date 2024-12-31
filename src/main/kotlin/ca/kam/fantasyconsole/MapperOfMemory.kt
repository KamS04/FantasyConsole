package ca.kam.fantasyconsole

import ca.kam.vmhardwarelibraries.memory.*

@OptIn(ExperimentalUnsignedTypes::class)
class MapperOfMemory(override val name: String): MemoryMapper {
    private val regions = mutableListOf<Device>()

    override fun map(device: MemoryDevice, start: UShort, size: UShort, remap: Boolean) {
        val memDev = Device(
            device,
            start,
            (start + size).s,
            size,
            remap
        )
        this.regions.add(memDev)
    }

    override fun slice(fAddress: UShort, tAddress: UShort): List<UByte> {
        val dev = findReadableDevice(fAddress)
        if (tAddress <= dev.end)
            return dev.device.slice(
                if (dev.remap) (fAddress - dev.start).s else fAddress,
                if (dev.remap) (tAddress - dev.start).s else tAddress
            )
        throw Exception("Cannot read beyond device (Cannot slice to multiple memory devices, EVEN if sequential read)")
    }

    override fun load(data: UByteArray, startAddress: UShort) {
        val dev = findWritableDevice(startAddress)
        if ((startAddress.toInt() + data.size).us < dev.end)
            return dev.device.load(
                data,
                if (dev.remap) (startAddress - dev.start).s else startAddress
            )
        throw Exception("Cannot write beyond device (Cannot load to multiple memory devices, EVEN if seqeuntial write)")
    }

    override val bit8 = object: Memory8 {
        override fun getUByte(address: UShort): UByte {
            return findReadableDevice(address).let {
                it.device.bit8.getUByte(
                    if (it.remap) (address - it.start).s else address
                )
            }
        }

        override fun setUByte(address: UShort, value: UByte): Boolean {
            return findWritableDevice(address).let {
                it.device.bit8.setUByte(
                    if (it.remap) (address - it.start).s else address,
                    value
                )
            }
        }
    }

    override val bit16 = object: Memory16 {
        override fun getUByte(address: UShort): UShort {
            return findReadableDevice(address).let {
                it.device.bit16.getUByte(
                    if (it.remap) (address - it.start).s else address
                )
            }
        }

        override fun setUByte(address: UShort, value: UShort): Boolean {
            return findWritableDevice(address).let {
                it.device.bit16.setUByte(
                    if (it.remap) (address - it.start).s else address,
                    value
                )
            }
        }
    }

    override fun findDevice(name: String): Device? {
        return regions.firstOrNull { it.device.name == name }
    }

    override fun findDevice(address: UShort): Device? {
        return regions.firstOrNull { it.start <= address && it.end > address}
    }

    fun findWritableDevice(address: UShort): Device {
        return findDevice(address) ?: throw Exception("Cannot write to address not mapped to a memory device: ${address.hexString()}")
    }

    fun findReadableDevice(address: UShort): Device {
        return findDevice(address) ?: throw Exception("Cannot read from address not mapped to a memory device: ${address.hexString()}")
    }
}