package com.example.disastermesh.core.data

/** 32-bit unsigned view of an Int */
inline val Int.u32: UInt
    get() = toUInt()                      // 0xFFFFFFFF -> 4_294_967_295u

/** 32-bit unsigned (low-word) view of a Long */
inline val Long.low32u: UInt
    get() = (this and 0xFFFF_FFFF).toUInt()