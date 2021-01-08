package com.github.lexcorp3439.net.lab2.dhcp;

public interface ConfigurableOption extends Option {
    void configure(Object configNode);

    @Override
    ConfigurableOptionDescription getDescription();
}
