package com.github.lexcorp3439.net.lab2.dhcp;

public interface ConfigurableOptionDescription extends OptionDescription {
    String getName();

    @Override
    ConfigurableOption produce();
}
