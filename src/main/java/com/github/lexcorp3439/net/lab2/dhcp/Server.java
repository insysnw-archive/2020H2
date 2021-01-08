package com.github.lexcorp3439.net.lab2.dhcp;

import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.spi.FileOptionHandler;
import org.kohsuke.args4j.spi.InetAddressOptionHandler;
import org.kohsuke.args4j.spi.IntOptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.DHCPMessageTypeOption;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.RebindingT2TimeValueOption;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.RenewalT1TimeValueOption;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.ServerIdentifierOption;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.ServerIdentifierOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.VendorClassIdentifierOption;
import com.github.lexcorp3439.net.lab2.dhcp.options.dhcp.VendorClassIdentifierOptionDescription;
import com.github.lexcorp3439.net.lab2.dhcp.util.Decoder;
import com.github.lexcorp3439.net.lab2.dhcp.util.Encoder;
import com.github.lexcorp3439.net.lab2.dhcp.util.IPAddress;

public final class Server {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    static public void main(String[] args) {
        new Server().entrypoint(args);
    }

    @org.kohsuke.args4j.Option(
            name = "-address",
            handler = InetAddressOptionHandler.class,
            usage = "IP4 address for server identification"
    )
    private Inet4Address address = (Inet4Address) InetAddress.getLoopbackAddress();

    @org.kohsuke.args4j.Option(
            name = "-listen",
            handler = InetAddressOptionHandler.class,
            usage = "IP4 address for binding"
    )
    private Inet4Address listen = null;

    @org.kohsuke.args4j.Option(
            name = "-broadcast",
            handler = InetAddressOptionHandler.class,
            usage = "IP4 address for binding"
    )
    private Inet4Address broadcast = null;

    @org.kohsuke.args4j.Option(
            name = "-port",
            handler = IntOptionHandler.class,
            usage = "UDP port number"
    )
    private int port = 67;

    @org.kohsuke.args4j.Option(
            name = "-config",
            handler = FileOptionHandler.class,
            usage = "Configuration path",
            required = true
    )
    private File config;

    private ServerConfiguration configuration;

    public void entrypoint(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            // parse the arguments.
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.err.println();
            System.exit(1);
        }

        try {
            configuration = ServerConfiguration.read(config.toPath());
            log.info(configuration.toString());
            operating();
        } catch (Exception e) {
            log.error("fatal error", e);
            System.exit(2);
        }
    }

    private Repository repository;
    private int serverIpAddress;

    private final byte[] mainBytes = new byte[DHCPPacket.PREFERRED_MAX_SIZE];

    private void operating() throws Exception {
        final DatagramSocket socket;
        final DatagramSocket hateSocket;

        repository = new Repository(configuration);
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
        if (networkInterface == null) {
            log.error("network interface not found");
            return;
        }

        if (broadcast == null) {
            broadcast = (Inet4Address) InetAddress.getByAddress(new byte[]{ -1, -1, -1, 1 });
        }

        Inet4Address listenAddress = null;
        if (listen != null) {
            listenAddress = listen;
        } else {
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                if (interfaceAddress.getAddress() instanceof Inet4Address) {
                    listenAddress = (Inet4Address) interfaceAddress.getBroadcast();
                    break;
                }
            }
            if (listenAddress == null) {
                log.error("broadcast address not found");
                return;
            }
        }

        socket = new DatagramSocket(port, listenAddress);
        {
            Inet4Address address = (Inet4Address) socket.getLocalAddress();
            if (!socket.getBroadcast()) {
                log.error("NOT A BROADCAST SOCKET");
                return;
            }
            log.info("attached to interface {}:{}", address, port);
        }
        socket.setBroadcast(true);

        serverIpAddress = IPAddress.fromBytes(address.getAddress());

        while (true) {
            final DatagramPacket received = new DatagramPacket(mainBytes, 0, mainBytes.length);
            socket.receive(received);
            Inet4Address address = (Inet4Address) received.getAddress(); // should be broadcast?...
            final int port = received.getPort();
            log.info("received from {}:{}", address, port);
            try {
                final ByteBuffer inBuffer = ByteBuffer.wrap(received.getData(), 0, received.getLength());
                final Decoder decoder = new Decoder(inBuffer);
                final DHCPPacket request = new DHCPPacket();
                request.decode(decoder);
                final DHCPPacket response =
                        processRequest(request, IPAddress.fromBytes(address.getAddress()) == 0xFFFF_FFFF);
                if (response != null) {
                    final int size = request.getMaximumResponseSize();
                    final byte[] bytes = (size <= mainBytes.length) ? mainBytes : new byte[size];
                    final ByteBuffer outBuffer = ByteBuffer.wrap(bytes, 0, size);
                    final Encoder encoder = new Encoder(outBuffer);
                    response.encode(encoder);
                    if (address.isAnyLocalAddress() || ((request.getFlags() & DHCPPacket.FLAG_BROADCAST) != 0)) {
                        address = broadcast;
                    }
                    final DatagramPacket toSend = new DatagramPacket(bytes, 0, size, address, port);
                    log.info("send to {}:{}", address, port);
                    socket.send(toSend);
                } else {
                    log.info("no response");
                }
            } catch (Exception e) {
                log.error("handling request", e);
            }
            repository.cleanup();
        }
    }

    private DHCPPacket processRequest(DHCPPacket request, boolean isBroadcast) {
        if (request.getOpcode() != DHCPPacket.PACKET_TYPE_REQUEST) {
            return null;
        }
        final DHCPMessageTypeOption.Types type = request.getMessageType();
        final ClientIdentifier identifier = request.getClientIdentifier();
        log.info("request {} from {}", type, identifier);
        switch (type) {
            case Discover:
                return handleDiscover(request);
            case Request:
                return handleRequest(request, isBroadcast);
            case Decline:
                return handleDecline(request);
            case Release:
                return handleRelease(request);
            case Inform:
                return handleInform(request);
            default:
                log.error("DHCP server doesn't respond on {} requests", type);
                return null;
        }
    }

    private DHCPPacket createPacket(DHCPPacket request, ClientIdentifier identifier) {
        DHCPPacket packet = new DHCPPacket();
        packet.setOpcode(DHCPPacket.PACKET_TYPE_REPLY);
        packet.setTransactionId(request.getTransactionId());
        packet.setBootFile(configuration.getBootFileName());
        packet.setServerHostname(configuration.getServerHostname());
        packet.setNextServerIpAddress(serverIpAddress);
        packet.setClientIdentifier(identifier);
        return packet;
    }

    private void addServerIdentifierOption(DHCPPacket packet) {
        ServerIdentifierOption identifier = new ServerIdentifierOption();
        identifier.setAddress(serverIpAddress);
        packet.addOption(identifier);
    }

    private void populateWithOptions(final DHCPPacket packet,
                                     final OptionsKeeper keeper,
                                     final byte[] parameters,
                                     boolean includeServerIdentifier
    ) {
        for (byte parameter : parameters) {
            Option option = keeper.findOption(parameter);
            if (option == null) {
                log.warn("unable to respond on {} option", Byte.toUnsignedInt(parameter));
            } else {
                packet.addOption(option);
            }
            if (includeServerIdentifier && parameter == ServerIdentifierOptionDescription.INSTANCE.getType()) {
                addServerIdentifierOption(packet);
            } else if (parameter == VendorClassIdentifierOptionDescription.INSTANCE.getType()) {
                VendorClassIdentifierOption vendorClassIdentifier = new VendorClassIdentifierOption();
                vendorClassIdentifier.setName("handtruth");
                packet.addOption(vendorClassIdentifier);
            }
        }
    }

    private DHCPPacket handleDiscover(final DHCPPacket request) {
        final ClientIdentifier identifier = request.getClientIdentifier();
        final Repository.Response answer = repository.offer(
                request.getClientIdentifier(), request.getPreferredAddress(), request.getPreferredLease()
        );
        final DHCPPacket response = createPacket(request, identifier);
        addServerIdentifierOption(response);
        response.setFlags(request.getFlags());
        response.setRelayAgentIpAddress(request.getRelayAgentIpAddress());
        if (answer.isError()) {
            response.setMessageType(DHCPMessageTypeOption.Types.NotAck);
            response.setErrorMessage(answer.getMessage());
        } else {
            response.setMessageType(DHCPMessageTypeOption.Types.Offer);
            response.setPreferredLease(answer.getLease());
            response.setYourIpAddress(answer.getRecord().getAddress());
            final byte[] parameters = request.getParameters();
            if (parameters != null) {
                populateWithOptions(response, answer.getRecord(), parameters, false);
            }
            response.setErrorMessage("offered");
        }
        return response;
    }

    private DHCPPacket handleRequest(final DHCPPacket request, boolean isBroadcast) {
        if (!isBroadcast && serverIpAddress != request.getServerIdentifier()) {
            return null;
        }
        final ClientIdentifier identifier = request.getClientIdentifier();
        final Repository.Response answer =
                repository.occupy(request.getClientIdentifier(), request.getPreferredLease());
        final DHCPPacket response = createPacket(request, identifier);
        addServerIdentifierOption(response);
        response.setFlags(request.getFlags());
        response.setRelayAgentIpAddress(request.getRelayAgentIpAddress());
        if (answer.isError()) {
            response.setMessageType(DHCPMessageTypeOption.Types.NotAck);
            response.setErrorMessage(answer.getMessage());
        } else {
            response.setMessageType(DHCPMessageTypeOption.Types.Ack);
            response.setPreferredLease(answer.getLease());
            response.setYourIpAddress(answer.getRecord().getAddress());
            final RenewalT1TimeValueOption renewal = new RenewalT1TimeValueOption();
            renewal.setSeconds(Math.max(0, answer.getLease() - 10));
            final RebindingT2TimeValueOption rebinding = new RebindingT2TimeValueOption();
            rebinding.setSeconds(answer.getLease());
            response.addOption(renewal);
            response.addOption(rebinding);
            final byte[] parameters = request.getParameters();
            if (parameters != null) {
                populateWithOptions(response, answer.getRecord(), parameters, false);
            } else {
                response.addOptions(answer.getRecord().getOptions());
            }
            response.setErrorMessage("assigned");
        }
        return response;
    }

    private DHCPPacket handleDecline(final DHCPPacket request) {
        final ClientIdentifier identifier = request.getClientIdentifier();
        repository.decline(identifier);
        return null;
    }

    private DHCPPacket handleRelease(final DHCPPacket request) {
        final ClientIdentifier identifier = request.getClientIdentifier();
        repository.release(identifier);
        return null;
    }

    private DHCPPacket handleInform(final DHCPPacket request) {
        final DHCPPacket response = createPacket(request, request.getClientIdentifier());
        response.setMessageType(DHCPMessageTypeOption.Types.Ack);
        response.setYourIpAddress(request.getClientIpAddress());
        final byte[] parameters = request.getParameters();
        if (parameters != null) {
            populateWithOptions(response, configuration, parameters, true);
        } else {
            response.addOptions(configuration.getDefaultOptions().values());
            addServerIdentifierOption(response);
        }
        return response;
    }
}
