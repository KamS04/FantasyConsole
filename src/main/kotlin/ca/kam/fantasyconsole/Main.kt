package ca.kam.fantasyconsole

import ca.kam.fantasyconsole.cmd.VMRuntime

fun main(args: Array<String>) = VMRuntime().main(arrayOf(
    "-D",
    "C:\\home\\code\\Kotlin\\TerminalDevice\\build\\libs\\TerminalDevice-1.0.jar",
    "-c",
    "C:\\home\\code\\Python\\Projects\\CartridgeBuilder\\whw.jar",
    "-m",
    "400",
//    "-d",
//    "--dump-memory"
))
