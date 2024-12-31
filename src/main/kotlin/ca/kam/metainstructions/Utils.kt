package ca.kam.metainstructions

fun String.asResourceStream() = object {}.javaClass.getResourceAsStream(this)
