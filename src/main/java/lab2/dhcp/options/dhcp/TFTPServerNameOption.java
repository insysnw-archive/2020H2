package lab2.dhcp.options.dhcp;

import lab2.dhcp.ConfigurableOption;
import lab2.dhcp.options.AbstractStringOption;
import lab2.dhcp.util.OptionsReader;

public final class TFTPServerNameOption extends AbstractStringOption implements ConfigurableOption {
    @Override
    public TFTPServerNameOptionDescription getDescription() {
        return TFTPServerNameOptionDescription.INSTANCE;
    }

    @Override
    public void configure(Object configNode) {
        setName(OptionsReader.readString(configNode));
    }
}
