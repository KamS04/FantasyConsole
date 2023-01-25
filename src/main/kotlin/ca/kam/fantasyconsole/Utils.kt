package ca.kam.fantasyconsole

fun UShort.hexString() = "0x" + toString(16).padStart(4, '0')

fun UByte.hexString() = "0x" + toString(16).padStart(4, '0')

fun UInt.hexString() = "0x" + toString(16).padStart(4, '0')

fun ULong.hexString() = "0x" + toString(16).padStart(4, '0')

fun Short.hexString() = "0x" + toString(16).padStart(4, '0')

fun Byte.hexString() = "0x" + toString(16).padStart(4, '0')

fun Int.hexString() = "0x" + toString(16).padStart(4, '0')

fun Long.hexString() = "0x" + toString(16).padStart(4, '0')

fun UByte.byteString() = toString(16).padStart(2, '0')

val Int.us: UShort
    get() = toUShort()

val Int.ub: UByte
    get() = toUByte()

val UInt.s: UShort
    get() = toUShort()

val UInt.b: UByte
    get() = toUByte()

val UShort.b: UByte
    get() = toUByte()

val UByte.s: UShort
    get() = toUShort()

infix fun UShort.shl(o: UShort): UShort = toUInt().shl(o.toInt()).s
infix fun UShort.shr(o: UShort): UShort = toUInt().shr(o.toInt()).s

infix fun UShort.shl(o: UByte): UShort = toUInt().shl(o.toInt()).s
infix fun UShort.shr(o: UByte): UShort = toUInt().shr(o.toInt()).s

@OptIn(ExperimentalUnsignedTypes::class)
fun addressToBytes(address: UShort): UByteArray = ubyteArrayOf( (address shr 8u).b, address.b  )
