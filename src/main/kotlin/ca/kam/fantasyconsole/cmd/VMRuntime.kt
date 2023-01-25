package ca.kam.fantasyconsole.cmd

import ca.kam.fantasyconsole.*
import ca.kam.fantasyconsole.machine.*
import ca.kam.metainstructions.InstructionBits.readInstructionsCSV
import ca.kam.vmhardwarelibraries.DeviceAsks
import ca.kam.vmhardwarelibraries.TechDevice
import ca.kam.vmhardwarelibraries.memory.RAM
import ca.kam.vmhardwarelibraries.memory.ROM
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import java.io.File
import java.lang.Integer.min
import kotlin.system.exitProcess

class VMRuntime: CliktCommand() {
    private val executor by option("-e", "--executor").file(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true,
    )

    private val extendedExecutor by option("--extended-executor").file(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true
    )

    private val instructionsFile by option("-i", "--instructions-file").file(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true
    )

    private val memorySize by option("-m", "--memory").int().default(256 * 256 - 1)

    private val cartridge by option("-c", "--cartridge").file(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true
    ).required()

    private val registers by option("-r", "--registers-file").file(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true
    )

    private val debugMode by option("-d", "--debug").flag()

    private val dumpUsedMemory by option("--dump-memory").flag()

    private val oldLoad by option("--old-loader").flag(default=false)

    private val devicesParameter by option("-D", "--devices").split(",")

    override fun run() {
        if (oldLoad) {
            noDevicesOldLoader()
        }

        withDevicesLoader()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun withDevicesLoader() {

        val memoryMapper = MapperOfMemory("FantasyConsole")

        var deviceListSize: UInt = 0u
        var techDevices: List<TechDevice> = emptyList()

        devicesParameter?.let { devices ->
            deviceListSize = (2 * devices.size).toUInt()
            techDevices = devices.map { loadDevice(File(it)) }
        }

        val asks = techDevices.map(TechDevice::deviceInfo)

        // interrupts 0, 1, 2, 3 + device interrupts
        val interruptSize = 10 + asks.sumOf { min(it.interrupts.size, 4) } * 2

        val systemInterruptCodeSize = 25
        val systemInterruptCodeAddress = interruptSize

        val deviceCodeSpaceSize = asks.sumOf { min(it.codeSpaceRequest.toInt(), 200) }
        val deviceCodeSpaceAddress = systemInterruptCodeAddress + systemInterruptCodeSize

        val deviceCommSpaceSize = asks.sumOf { min(it.commBufferRequest.toInt(), 50) }
        val deviceCommSpaceAddress = deviceCodeSpaceAddress + deviceCodeSpaceSize

        val deviceNameArraySize = techDevices.size
        val deviceNameArrayAddress = deviceCommSpaceAddress + deviceCommSpaceSize
        val deviceCommArraySize = techDevices.size * 2
        val deviceCommArrayAddress = deviceNameArrayAddress + deviceNameArraySize

        val deviceInterruptArraySize = asks.sumOf { min(it.interrupts.size, 4) }
        val deviceInterruptsArrayAddress = deviceCommArrayAddress + deviceCommArraySize

        val totalSystemOverheadMemory = deviceInterruptsArrayAddress + deviceInterruptArraySize
        val programMemorySize = memorySize - totalSystemOverheadMemory

        // region system interrupt rom
        val int1and2ByteArray = ubyteArrayOf(
            *addressToBytes(deviceCommArrayAddress.toUShort()), // data16 commList = { $0000 }
            *addressToBytes(deviceNameArrayAddress.toUShort()), // data16 devList = { $0000 }
            *addressToBytes(deviceInterruptsArrayAddress.toUShort()), // data16 devIntList = { $0000 }
            0x20u, 0x00u, 0x02u, 0x02u,   // mul $2, r1
            0x3fu, 0x00u, 0x00u, 0x00u,   // add [!commList], acu
            0xfcu,                        // rti
            0x13u, 0x00u, 0x02u, 0x00u,   // mov &[!devList], acu
            0xfcu,                        // rti
            0x13u, 0x00u, 0x04u, 0x01u,   // move &[!devIntList], acu
            0xfcu,                        // rti
        )

        memoryMapper.map(
            ROM(int1and2ByteArray.size, "System Interrupts")
                .apply {
                    load(int1and2ByteArray, 0u)
                },
            systemInterruptCodeAddress.toUShort(),
            int1and2ByteArray.size.toUShort(),
            true
        );
        //endregion

        //region cartridge
        val cartridge = loadFantasyCartridge(cartridge)
        val programMemory = RAM(programMemorySize, "Program Memory")
        cartridge.loadInto(programMemory, programMemorySize)

        memoryMapper.map(programMemory, totalSystemOverheadMemory.us, programMemorySize.us, true)
        //endregion

        var cBuff = 0
        val deviceCodeAddresses = asks.map {
            val x = deviceCodeSpaceAddress + cBuff
            cBuff += min(it.codeSpaceRequest.toInt(), 200)
            x.us
        }
        cBuff = 0
        val deviceCommAddresses = asks.map {
            val x = deviceCommSpaceAddress + cBuff
            cBuff += min(it.commBufferRequest.toInt(), 50)
            x.us
        }

        //region interrupt vector
        val overheadInterrupts = 5
        val interruptAddresses = ubyteArrayOf(
            *addressToBytes((cartridge.startAddress + totalSystemOverheadMemory.us).toUShort()),
            *addressToBytes((memorySize - 2).us),
            *addressToBytes((systemInterruptCodeAddress + 0x0006).us),
            *addressToBytes((systemInterruptCodeAddress + 0x000f).us),
            *addressToBytes((systemInterruptCodeAddress + 0x0014).us),
            *asks.flatMapIndexed { idx, ask ->
                ask.interrupts.map { (it + deviceCodeAddresses[idx]).s }
            }.flatMap(::addressToBytes).toUByteArray()
        )
        assert(interruptAddresses.size == interruptSize)
        val interruptVectorROM = ROM(interruptSize, "Interrupt Vector")
            .apply {
                load(interruptAddresses, 0u)
            }
        memoryMapper.map(interruptVectorROM, 0u, interruptSize.us, false)
        //endregion

        //region device arrays
        val deviceNameArray = asks.map(DeviceAsks::name).toUByteArray()
        assert(deviceNameArraySize == deviceNameArray.size)
        val deviceNameROM = ROM(deviceNameArraySize, "Device Name Array")
            .apply {
                load(deviceNameArray, 0u)
            }
        memoryMapper.map(
            deviceNameROM,
            deviceNameArrayAddress.us,
            deviceNameArraySize.us,
            true
        )
        val deviceCommArray = deviceCommAddresses.flatMap(::addressToBytes).toUByteArray()
        assert(deviceCommArraySize == deviceCommArray.size)
        val deviceCommROM = ROM(deviceCommArraySize, "Device Comm Array")
            .apply {
                load(deviceCommArray, 0u)
            }
        memoryMapper.map(
            deviceCommROM,
            deviceCommArrayAddress.us,
            deviceCommArraySize.us,
            true
        )

        var intC = overheadInterrupts
        val deviceIdToFirstInterrupt = UByteArray(deviceInterruptArraySize)
        asks.forEachIndexed { index, deviceAsks ->
            if (deviceAsks.interrupts.isNotEmpty()) {
                deviceIdToFirstInterrupt[index] = intC.toUByte()
                intC += min( deviceAsks.interrupts.size, 4 )
            } else {
                deviceIdToFirstInterrupt[index] = 0u
            }
        }

        val deviceInterruptArray = ROM(deviceInterruptArraySize, "Device Interrupts Array")
            .apply {
                load(deviceIdToFirstInterrupt, 0u)
            }
        memoryMapper.map(
            deviceInterruptArray,
            deviceInterruptsArrayAddress.us,
            deviceInterruptArraySize.us,
            true
        )
        //endregion

        //region device memory arrays
        techDevices.zip(asks).forEachIndexed { idx, pair ->
            val devCodeRAM = RAM(
                min(pair.second.codeSpaceRequest.toInt(), 200),
                "Dev $idx CodeSpace"
            ).apply {
                val bytes = pair.first.getCode()
                bytes[0] = idx.ub
                bytes[1] = (deviceCommAddresses[idx] shr 8u).b
                bytes[2] = deviceCommAddresses[idx].b
                load(bytes, 0u)
            }
            memoryMapper.map(
                devCodeRAM,
                deviceCodeAddresses[idx],
                min(pair.second.codeSpaceRequest.toInt(), 200).us,
                true
            )

            val devCommRAM = RAM(
                min(pair.second.commBufferRequest.toInt(), 50),
                "Dev $idx Communication Buffer"
            )
            pair.first.lockBuffer(devCommRAM)
            memoryMapper.map(
                devCommRAM,
                deviceCommAddresses[idx],
                min(pair.second.commBufferRequest.toInt(), 50).us,
                true
            )
        }
        //endregion

        if (debugMode) {
            println("Memory Layout")
            println("Interrupt Vector ${0.us.hexString()}")
            println("System Interrupts Code ${systemInterruptCodeAddress.hexString()}")
            deviceCodeAddresses.forEachIndexed { idx, address ->
                println("Device 0x${asks[idx].name.hexString()} Code ${address.hexString()}")
            }
            deviceCommAddresses.forEachIndexed { idx, address ->
                println("Device 0x${asks[idx].name.hexString()} Comm Buf ${address.hexString()}")
            }
            println("Device Name Array ${deviceNameArrayAddress.hexString()}")
            println("Device Comm Array ${deviceCommArrayAddress.hexString()}")
            println("Device Interrupt Array ${deviceInterruptsArrayAddress.hexString()}")
            println("Program Memory ${totalSystemOverheadMemory.hexString()}")
        }

        if (dumpUsedMemory) {
            println("Dumping System Memory")
            for (i in 0 until totalSystemOverheadMemory) {
                if (i % 5 == 0) {
                    print("\n")
                    print("${i.hexString()}: ")
                }
                print("${memoryMapper.bit8[i.us].byteString()} ")
            }
            print("\nEnd System Memory\n")
        }

        val builder = CPUBuilder()
        builder.memory(memoryMapper)
        builder.debug(debugMode)
        builder.interrupt(0x00u)
        techDevices.forEach(builder::attach)

        registers?.let {
            val rF = loadRegistersFile(it)
            builder.registersNames(rF.registerNames)
            builder.requiredRegisters(rF.requiredRegisters)
        }

        instructionsFile?.let {
            builder.instructions(
                readInstructionsCSV(it.toURI().toString())
            )
        }

        executor?.let {
            builder.executor(
                loadExecutor(it)
            )
        }

        extendedExecutor?.let {
            if (executor != null) {
                throw Exception("Cannot extend custom executor")
            }
            builder.executor(
                loadExtendedExecutor(it)
            )
        }

        val cpu = builder.build()
        cartridge.initializeGraphics(cpu)
        if (cartridge.isForkedGraphics) {
            cpu.run()
            cartridge.exitGraphics()
            exitProcess(0)
        }
        else {
            cartridge.startCPU(cpu)
            cpu.run()
        }
    }

    fun noDevicesOldLoader() {
        val builder = CPUBuilder()
        val memoryMapper = MapperOfMemory("FantasyConsole")
        memoryMapper.map(
            RAM(memorySize, "BasicRAM"),
            0u,
            memorySize.us,
            false
        )

        val cartridge = loadCartridge(cartridge)
        cartridge.loadInto(memoryMapper, memorySize)
        builder.memory(memoryMapper)
        builder.debug(debugMode)
        builder.interrupt(cartridge.interruptVectorAddress)

        registers?.let {
            val rF = loadRegistersFile(it)
            builder.registersNames(rF.registerNames)
            builder.requiredRegisters(rF.requiredRegisters)
        }

        instructionsFile?.let {
            builder.instructions(
                readInstructionsCSV(it.toURI().toString())
            )
        }

        executor?.let {
            builder.executor(
                loadExecutor(it)
            )
        }

        extendedExecutor?.let {
            if (executor != null) {
                throw Exception("Cannot extend custom executor")
            }

            builder.executor(
                loadExtendedExecutor(it)
            )
        }

        val cpu = builder.build()

        cartridge.initializeGraphics(cpu)
        if (cartridge.isForkedGraphics) {
            cpu.run()
            cartridge.exitGraphics()
            exitProcess(0)
        }
        else
            cartridge.startCPU(cpu)
            cpu.run()
    }
}