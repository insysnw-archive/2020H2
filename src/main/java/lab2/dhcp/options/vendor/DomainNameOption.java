package lab2.dhcp.options.vendor;

import lab2.dhcp.ConfigurableOption;
import lab2.dhcp.options.AbstractStringOption;
import lab2.dhcp.util.OptionsReader;

public final class DomainNameOption extends AbstractStringOption implements ConfigurableOption {
    @Override
    public DomainNameOptionDescription getDescription() {
        return DomainNameOptionDescription.INSTANCE;
    }

    @Override
    public void configure(Object configNode) {
        setName(OptionsReader.readString(configNode));
    }
}
