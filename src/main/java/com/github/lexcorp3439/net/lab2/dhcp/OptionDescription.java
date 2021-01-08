package com.github.lexcorp3439.net.lab2.dhcp;

public interface OptionDescription {
    byte getType();
    Option produce();
}
