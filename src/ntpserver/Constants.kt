package ntpserver

import kotlin.time.ExperimentalTime
import kotlin.time.seconds

const val DEFAULT_ADDRESS = "localhost"
const val DEFAULT_PORT = 123
const val VERSION: Byte = 4
const val MINPOLL: Byte = 4
const val MAXPOLL: Byte = 17

@ExperimentalTime
val MAXDISP = 16.seconds

@ExperimentalTime
val MINDISP = .005.seconds

const val NTP_DELTA = 2208988800
