package ca.kam.fantasyconsole.machine

import ca.kam.fantasyconsole.cpu.DefaultExecutor
import ca.kam.fantasyconsole.cpu.DefaultExtendedExecutor
import ca.kam.vmhardwarelibraries.Cartridge
import ca.kam.vmhardwarelibraries.FantasyCartridge
import ca.kam.vmhardwarelibraries.RegistersFile
import ca.kam.vmhardwarelibraries.TechDevice
import ca.kam.vmhardwarelibraries.cpu.Executor
import ca.kam.vmhardwarelibraries.cpu.ExtendedExecutor
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

fun File.createLoader() = URLClassLoader(
    arrayOf(toURI().toURL()),
    object {}.javaClass.classLoader
)

fun loadExecutor(file: File): Executor {
    val x = object {}.javaClass.classLoader
    val jar = JarFile(file)
    val executorClsName = (jar.manifest.mainAttributes.getValue("Executor") as String?) ?: throw Exception("No Executor Specified in manifest")
    val loader = file.createLoader()
    val executorClass = loader.loadClass(executorClsName)
    if (!executorClass.interfaces.contains(Executor::class.java))
        throw Exception("Specified Executor class is not of Type Executor")

    return executorClass.getConstructor().newInstance() as Executor
}

fun loadExtendedExecutor(file: File): Executor {
    val jar = JarFile(file)
    val extendedExecutorClsName = (jar.manifest.mainAttributes.getValue("ExtendedExecutor") as String?) ?: throw Exception("No ExtendedExecutor Specified in manifest")
    val loader = file.createLoader()
    val extendedExecutorClass = loader.loadClass(extendedExecutorClsName)
    if (!extendedExecutorClass.interfaces.contains(ExtendedExecutor::class.java))
        throw Exception("Specified Extended Executor class is not of Type ExtendedExecutor")

    return DefaultExtendedExecutor(
        DefaultExecutor,
        extendedExecutorClass.getConstructor().newInstance() as ExtendedExecutor
    )
}

fun loadDevice(file: File): TechDevice {
    val jar = JarFile(file)
    val deviceClsName = (jar.manifest.mainAttributes.getValue("Device") as String?) ?: throw Exception("No Device specified in manifest")
    val loader = file.createLoader()
    val deviceClass = loader.loadClass(deviceClsName)
    if (!deviceClass.interfaces.contains(TechDevice::class.java))
        throw Exception("Specified Device class is not of Type TechDevice")

    return deviceClass.getConstructor().newInstance() as TechDevice
}

fun loadCartridge(file: File): Cartridge {
    val jar = JarFile(file)
    val cartridgeClsName = (jar.manifest.mainAttributes.getValue("Cartridge") as String?) ?: throw Exception("No Cartridge specified in manifest")
    val loader = file.createLoader()
    val cartridgeClass = loader.loadClass(cartridgeClsName)
    if (!cartridgeClass.interfaces.contains(Cartridge::class.java))
        throw Exception("Specified Cartridge class is not of Type Cartridge")

    return cartridgeClass.getConstructor().newInstance() as Cartridge
}

fun loadFantasyCartridge(file: File): FantasyCartridge {
    val jar = JarFile(file)
    val cartridgeClsName = (jar.manifest.mainAttributes.getValue("Cartridge") as String?) ?: throw Exception("No Cartridge specified in manifest")
    val loader = file.createLoader()
    val cartridgeClass = loader.loadClass(cartridgeClsName)
    if (!cartridgeClass.interfaces.contains(FantasyCartridge::class.java))
        throw Exception("Specified Cartridge class is not of Type Cartridge")

    return cartridgeClass.getConstructor().newInstance() as FantasyCartridge
}

fun loadRegistersFile(file: File): RegistersFile {
    val jar = JarFile(file)
    val registersFileClsName = (jar.manifest.mainAttributes.getValue("RegistersFile") as String?) ?: throw Exception("No Registers File specified in manifest")
    val loader = file.createLoader()
    val registersClass = loader.loadClass(registersFileClsName)
    if (!registersClass.interfaces.contains(RegistersFile::class.java))
        throw Exception("Specified RegistersFile class is not of Type RegistersFile")

    return registersClass.getConstructor().newInstance() as RegistersFile
}
