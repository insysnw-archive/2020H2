package com.github.lexcorp3439.net.lab2.dhcp;

import java.util.HashMap;
import java.util.Map;

import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.BootFileNameOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.ClientIdentifierOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.DHCPMessageTypeOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.IpAddressLeaseTimeOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.MaximumDHCPMessageSizeOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.MessageOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.OptionOverloadOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.ParameterRequestListOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.RebindingT2TimeValueOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.RenewalT1TimeValueOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.RequestedIpAddressOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.ServerIdentifierOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.TFTPServerNameOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.VendorClassIdentifierOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.ip.BroadcastAddressOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.ip.InterfaceMTUOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.vendor.DomainNameOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.vendor.DomainNameServerOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.vendor.HostNameOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.vendor.RouterOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.vendor.SubnetMaskOptionDescription;

public final class Options {
    private Options() {

    }

    private static final Map<Byte, OptionDescription> all = new HashMap<>();
    private static final Map<String, ConfigurableOptionDescription> named = new HashMap<>();

    public static OptionDescription get(byte type) {
        return all.get(type);
    }

    public static ConfigurableOptionDescription get(String name) {
        return named.get(name);
    }

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
