package com.mashiverse.utils.helpers

fun ByteArray.indexOfSequence(sequence: ByteArray, startIndex: Int = 0): Int {
    if (sequence.isEmpty()) return -1
    for (i in startIndex..this.size - sequence.size) {
        var found = true
        for (j in sequence.indices) {
            if (this[i + j] != sequence[j]) {
                found = false
                break
            }
        }
        if (found) return i
    }
    return -1
}

fun ByteArray.readUShortBE(offset: Int): Int {
    return ((this[offset].toInt() and 0xFF) shl 8) or (this[offset + 1].toInt() and 0xFF)
}