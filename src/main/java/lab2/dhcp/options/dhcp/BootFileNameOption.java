package lab2.dhcp.options.dhcp;

import lab2.dhcp.options.AbstractStringOption;

public final class BootFileNameOption extends AbstractStringOption {
    @Override
    public BootFileNameOptionDescription getDescription() {
        return BootFileNameOptionDescription.INSTANCE;
    }
}
