package lab2.dhcp;

import lab2.dhcp.options.dhcp.*;
import lab2.dhcp.options.ip.BroadcastAddressOptionDescription;
import lab2.dhcp.options.ip.InterfaceMTUOptionDescription;
import lab2.dhcp.options.vendor.*;

import java.util.HashMap;
import java.util.Map;

public final class Options {
    private Options() {

    }

    private static final Map<Byte, OptionDescription> all = new HashMap<>();
    private static final Map<String, ConfigurableOptionDescription> named = new HashMap<>();

    public static OptionDescription get(byte type) {
        return all.get(type);
    }
    public static ConfigurableOptionDescription get(String name) { return named.get(name); }

    static {
        OptionDescription[] array = {
                SubnetMaskOptionDescription.INSTANCE,
                RouterOptionDescription.INSTANCE,
                DomainNameServerOptionDescription.INSTANCE,
                HostNameOptionDescription.INSTANCE,
                DomainNameOptionDescription.INSTANCE,
                InterfaceMTUOptionDescription.INSTANCE,
                BroadcastAddressOptionDescription.INSTANCE,
                BootFileNameOptionDescription.INSTANCE,
                ClientIdentifierOptionDescription.INSTANCE,
                DHCPMessageTypeOptionDescription.INSTANCE,
                IpAddressLeaseTimeOptionDescription.INSTANCE,
                MaximumDHCPMessageSizeOptionDescription.INSTANCE,
                MessageOptionDescription.INSTANCE,
                OptionOverloadOptionDescription.INSTANCE,
                ParameterRequestListOptionDescription.INSTANCE,
                RebindingT2TimeValueOptionDescription.INSTANCE,
                RenewalT1TimeValueOptionDescription.INSTANCE,
                RequestedIpAddressOptionDescription.INSTANCE,
                ServerIdentifierOptionDescription.INSTANCE,
                TFTPServerNameOptionDescription.INSTANCE,
                VendorClassIdentifierOptionDescription.INSTANCE
        };
        for (OptionDescription description : array) {
            all.put(description.getType(), description);
            if (description instanceof ConfigurableOptionDescription) {
                ConfigurableOptionDescription configurable = (ConfigurableOptionDescription) description;
                named.put(configurable.getName(), configurable);
            }
        }
    }
}
