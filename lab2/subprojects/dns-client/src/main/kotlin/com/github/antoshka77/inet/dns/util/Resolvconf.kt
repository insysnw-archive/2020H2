package com.github.antoshka77.inet.dns.util

import java.io.File

fun getNameServer(): String {
    val conf = File("/etc/resolv.conf")
    if (conf.exists()) {
        val nameserver = conf.readLines()
            .filter { it.trim().startsWith("nameserver") }
            .map {
                val index = it.indexOf('#')
                if (index == -1) {
                    it
                } else {
                    it.substring(0, index)
                }
            }.firstOrNull()
        checkNotNull(nameserver) { "no nameserver configuration option" }
        val address = nameserver.split(' ').filter { it.isNotEmpty() }
        check(address.size >= 2) { "nameserver empty" }
        check(address[0] == "nameserver")
        return address[1]
    }
    error("file /etc/resolv.conf does not exists")
}
