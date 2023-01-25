package ca.kam.fantasyconsole.cpu

import ca.kam.vmhardwarelibraries.cpu.RequiredRegisters

object DefaultRegisters: RequiredRegisters {
    override val IP: String = "ip"
    override val ACU: String = "acu"
    override val SP: String = "sp"
    override val FP: String = "fp"
    override val IM: String = "im"

    val defaultRegisters = listOf(
        "ip", "acu",
        "r1", "r2", "r3", "r4",
        "r5", "r6", "r7", "r8",
        "sp", "fp", "mb", "im"
    )
}
