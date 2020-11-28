package com.github.antoshka77.inet.ntp

import com.github.antoshka77.inet.Int8
import kotlin.time.seconds

// NTP port number
const val PORT = 123

// NTP version number
const val VERSION: Int8 = 4

// frequency tolerance PHI (s/s)
const val TOLERANCE = 15e-6

// minimum poll exponent (16 s)
const val MINPOLL: Int8 = 4

// maximum poll exponent (36 h)
const val MAXPOLL: Int8 = 17

// maximum dispersion (16 s)
val MAXDISP = 16.seconds

// minimum dispersion increment (s)
val MINDISP = .005.seconds

// distance threshold (1 s)
const val MAXDIST = 1

// maximum stratum number
const val MAXSTRAT: Int8 = 16
