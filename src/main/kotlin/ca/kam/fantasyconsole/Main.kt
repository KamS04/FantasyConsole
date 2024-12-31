package ca.kam.fantasyconsole

import ca.kam.fantasyconsole.cmd.VMRuntime

fun main(args: Array<String>) = VMRuntime().main(arrayOf(
    "-D",
    "C:\\home\\code\\Kotlin\\TerminalDevice\\build\\libs\\TerminalDevice-1.0.jar",
    "-d",
    "--code",
    "C:\\home\\Code\\C\\DoctorWkt\\out.a",
    "-m",
    "2400",
))
