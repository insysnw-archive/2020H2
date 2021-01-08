package com.github.lexcorp3439.net.lab2.dhcp.options.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.options.AbstractStringOption;

public final class MessageOption extends AbstractStringOption {
    @Override
    public MessageOptionDescription getDescription() {
        return MessageOptionDescription.INSTANCE;
    }
}
