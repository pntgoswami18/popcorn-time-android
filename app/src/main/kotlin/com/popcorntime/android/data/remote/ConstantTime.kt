package com.popcorntime.android.data.remote

/**
 * Compares two strings in time proportional to the length of [expected] only,
 * so neither an early mismatch nor a length mismatch leaks timing information
 * about the expected secret.
 */
internal fun constantTimeEquals(provided: String, expected: String): Boolean {
    val len = expected.length
    var diff = provided.length xor len // non-zero if lengths differ
    for (i in 0 until len) {
        val a = if (i < provided.length) provided[i].code else 0
        diff = diff or (a xor expected[i].code)
    }
    return diff == 0
}
