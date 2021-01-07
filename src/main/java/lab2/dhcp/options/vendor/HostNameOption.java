package lab2.dhcp.options.vendor;

import lab2.dhcp.ConfigurableOption;
import lab2.dhcp.options.AbstractStringOption;
import lab2.dhcp.util.OptionsReader;

public final class HostNameOption extends AbstractStringOption implements ConfigurableOption {
    @Override
    public HostNameOptionDescription getDescription() {
        return HostNameOptionDescription.INSTANCE;
    }

    @Override
    public void configure(Object configNode) {
        setName(OptionsReader.readString(configNode));
    }
}
