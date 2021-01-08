package com.github.lexcorp3439.net.lab2.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.util.IPAddress;
import com.github.lexcorp3439.net.lab2.dhcp.util.OptionsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tomlj.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class ServerConfiguration implements OptionsKeeper {
    private static final Logger log = LoggerFactory.getLogger(ServerConfiguration.class);

    private ServerConfiguration() {

    }

    private int ipRangeStart;
    private int ipRangeStop;
    private String serverHostname;
    private String bootFileName;
    private int leaseTime;
    private int maxLeaseTime;

    public int getIpRangeStart() {
        return ipRangeStart;
    }

    public int getIpRangeStop() {
        return ipRangeStop;
    }

    public String getServerHostname() {
        return serverHostname;
    }

    public String getBootFileName() {
        return bootFileName;
    }

    public long getLeaseTime() {
        return Integer.toUnsignedLong(leaseTime);
    }

    public long getMaxLeaseTime() {
        return Integer.toUnsignedLong(maxLeaseTime);
    }

    private final Map<Byte, ConfigurableOption> defaultOptions = new HashMap<>();
    private final Map<Byte, Option> rdDefaultOptions = Collections.unmodifiableMap(defaultOptions);

    public Map<Byte, Option> getDefaultOptions() {
        return rdDefaultOptions;
    }

    @Override
    public Option findOption(byte type) {
        return defaultOptions.get(type);
    }

    private final ArrayList<HostConfiguration> hosts = new ArrayList<>();
    private final List<HostConfiguration> rdHosts = Collections.unmodifiableList(hosts);

    public List<HostConfiguration> getHosts() {
        return rdHosts;
    }

    public static class HostConfiguration {
        private final Map<Byte, ConfigurableOption> options = new HashMap<>();
        private final Map<Byte, Option> rdOptions = Collections.unmodifiableMap(options);
        private ClientIdentifier identifier;
        private int address;

        public Map<Byte, Option> getOptions() {
            return rdOptions;
        }

        public ClientIdentifier getIdentifier() {
            return identifier;
        }

        public int getAddress() {
            return address;
        }

        @Override
        public String toString() {
            return "HostConfiguration{" +
                    "options=" + optionsToString(options.values()) +
                    ", identifier=" + identifier +
                    ", address=" + IPAddress.ipAddressToString(address) +
                    '}';
        }
    }

    private static List<ConfigurableOption> readOptions(TomlTable table) {
        List<ConfigurableOption> options = new ArrayList<>(table.size());
        for (String key : table.keySet()) {
            ConfigurableOptionDescription description = Options.get(key);
            if (description == null) {
                log.warn("option type \"{}\" not found", key);
            } else {
                ConfigurableOption option = description.produce();
                option.configure(table.get(key));
                options.add(option);
            }
        }
        return options;
    }

    private static HostConfiguration readHost(TomlTable table) {
        String hwAddress = table.getString("hardware-ethernet");
        if (hwAddress == null) {
            throw new IllegalStateException("host configuration must contain \"hardware-ethernet\" parameter");
        }
        String address = table.getString("address");
        if (address == null) {
            throw new IllegalStateException("host configuration must contain \"address\" parameter");
        }
        HostConfiguration host = new HostConfiguration();
        host.identifier = MACAddressClientIdentifier.parse(hwAddress);
        host.address = OptionsReader.readIPAddress(address);
        for (ConfigurableOption option : readOptions(table.getTableOrEmpty("options"))) {
            host.options.put(option.getDescription().getType(), option);
        }
        return host;
    }

    public static ServerConfiguration read(Path file) throws IOException {
        final TomlParseResult result = Toml.parse(file);
        if (result.hasErrors()) {
            for (TomlParseError error : result.errors()) {
                log.error("configuration parsing error", error);
            }
            throw new RuntimeException();
        }
        final ServerConfiguration configuration = new ServerConfiguration();
        final int start = OptionsReader.readIPAddress(result.getString("range.start"));
        final int stop = OptionsReader.readIPAddress(result.getString("range.stop"));
        if (start < stop) {
            configuration.ipRangeStart = start;
            configuration.ipRangeStop = stop;
        } else {
            configuration.ipRangeStart = stop;
            configuration.ipRangeStop = start;
        }
        for (ConfigurableOption option : readOptions(result.getTableOrEmpty("options"))) {
            configuration.defaultOptions.put(option.getDescription().getType(), option);
        }
        final Long leaseTime = result.getLong("default-lease-time");
        configuration.leaseTime = (leaseTime == null) ? 1000 : (int) (long) leaseTime;
        final Long maxLeaseTime = result.getLong("max-lease-time");
        configuration.maxLeaseTime = (maxLeaseTime == null) ? 7200 : (int) (long) maxLeaseTime;
        configuration.serverHostname = result.getString("server-hostname");
        configuration.bootFileName = result.getString("boot-file-name");
        TomlArray hostsArray = result.getArray("host");
        if (hostsArray != null) {
            final int hostsCount = hostsArray.size();
            configuration.hosts.ensureCapacity(hostsCount);
            for (int i = 0; i < hostsCount; ++i) {
                configuration.hosts.add(readHost(hostsArray.getTable(i)));
            }
        }
        return configuration;
    }

    private static String optionsToString(Collection<ConfigurableOption> options) {
        if (options.isEmpty()) {
            return "{ }";
        }
        Iterator<ConfigurableOption> iterator = options.iterator();
        StringBuilder result = new StringBuilder("{");
        ConfigurableOption first = iterator.next();
        result.append(first.getDescription().getName());
        result.append('=');
        result.append(first.toString());
        while (iterator.hasNext()) {
            result.append(", ");
            ConfigurableOption option = iterator.next();
            result.append(option.getDescription().getName());
            result.append('=');
            result.append(option.toString());
        }
        result.append('}');
        return result.toString();
    }

    @Override
    public String toString() {
        return "ServerConfiguration{" +
                "ipRangeStart=" + IPAddress.ipAddressToString(ipRangeStart) +
                ", ipRangeStop=" + IPAddress.ipAddressToString(ipRangeStop) +
                ", serverHostname='" + serverHostname + '\'' +
                ", bootFileName='" + bootFileName + '\'' +
                ", leaseTime=" + Integer.toUnsignedString(leaseTime) +
                ", maxLeaseTime=" + Integer.toUnsignedString(maxLeaseTime) +
                ", defaultOptions=" + optionsToString(defaultOptions.values()) +
                ", hosts=" + hosts +
                '}';
    }
}
