package lab2.dhcp;

import lab2.dhcp.options.dhcp.*;
import lab2.dhcp.util.DHCPIllegalFormatException;
import lab2.dhcp.util.Decoder;
import lab2.dhcp.util.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class DHCPPacket {
    private static final Logger log = LoggerFactory.getLogger(DHCPPacket.class);

    public static final int OPTIONS_PREFERRED_MAX_SIZE = 312;
    public static final int PREFERRED_MAX_SIZE = 576;
    public static final int HEAD_SIZE = 236;

    public static final byte PACKET_TYPE_REQUEST = 1;
    public static final byte PACKET_TYPE_REPLY = 2;

    static final short FLAG_BROADCAST = (short) 0x80_00;

    private static final int OPTIONS_MAGIC = 0x63_82_53_63; // 63.82.53.63

    private static final int CLIENT_IDENTIFIER_FIELD_SIZE = 16;
    private static final int SERVER_HOSTNAME_SIZE = 64;
    private static final int BOOT_FILE_SIZE = 128;

    private byte opcode;                                        // op(1)
    private byte hardwareAddressType;                           // htype(1)
    private byte hardwareAddressLength;                         // glen(1)
    private byte hops;                                          // hops(1)
    private int transactionId;                                  // xid(4)
    private int elapsedSeconds;                                 // secs(2)
    private short flags;                                        // flags(2)
    private int clientIpAddress;                                // ciaddr(4)
    private int yourIpAddress;                                  // yiaddr(4)
    private int nextServerIpAddress;                            // siaddr(4)
    private int relayAgentIpAddress;                            // giaddr(4)
    private final byte[] clientHardwareAddress = new byte[16];  // chaddr(16)
    private String serverHostname;                              // sname(64)
    private String bootFile;                                    // file(128)
    private final List<Option> options = new ArrayList<>();     // options(var)

    public byte getOpcode() {
        return opcode;
    }

    public void setOpcode(byte opcode) {
        this.opcode = opcode;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setElapsedSeconds(long elapsedSeconds) {
        this.elapsedSeconds = (int) elapsedSeconds;
    }

    public long getElapsedSeconds() {
        return Integer.toUnsignedLong(elapsedSeconds);
    }

    public void setFlags(short flags) {
        this.flags = flags;
    }

    public short getFlags() {
        return flags;
    }

    public void setClientIpAddress(int clientIpAddress) {
        this.clientIpAddress = clientIpAddress;
    }

    public int getClientIpAddress() {
        return clientIpAddress;
    }

    public void setYourIpAddress(int yourIpAddress) {
        this.yourIpAddress = yourIpAddress;
    }

    public int getYourIpAddress() {
        return yourIpAddress;
    }

    public void setNextServerIpAddress(int nextServerIpAddress) {
        this.nextServerIpAddress = nextServerIpAddress;
    }

    public int getNextServerIpAddress() {
        return nextServerIpAddress;
    }

    public void setRelayAgentIpAddress(int relayAgentIpAddress) {
        this.relayAgentIpAddress = relayAgentIpAddress;
    }

    public int getRelayAgentIpAddress() {
        return relayAgentIpAddress;
    }

    public void setServerHostname(String serverHostname) {
        this.serverHostname = serverHostname;
    }

    public String getServerHostname() {
        return serverHostname;
    }

    public void setBootFile(String bootFile) {
        this.bootFile = bootFile;
    }

    public String getBootFile() {
        return bootFile;
    }

    private int strLen(final byte[] bytes, final int start, final int max) {
        int i;
        for (i = start; i < max && bytes[i] != '\0'; ++i) ;
        return i;
    }

    private String getString(final byte[] bytes, final int start, final int max) {
        final int size = strLen(bytes, start, max);
        return size == 0 ? null : new String(bytes, start, size, StandardCharsets.US_ASCII);
    }

    private static final byte PAD_OPTION = 0;
    private static final byte END_OPTION = -1;

    private List<Option> decodeOptions(Decoder decoder) {
        List<Option> result = new ArrayList<>();
        byte[] bytes = new byte[256];
        while (decoder.remaining() != 0) {
            byte type = decoder.getByte();
            switch (type) {
                case PAD_OPTION:
                    break;
                case END_OPTION:
                    return result;
                default: {
                    OptionDescription description = Options.get(type);
                    int size = decoder.getByte() & 0xFF;
                    if (description == null) {
                        decoder.discard(size);
                    } else {
                        decoder.getBytes(bytes, 0, size);
                        ByteBuffer subBuffer = ByteBuffer.wrap(bytes, 0, size);
                        Decoder subDecoder = new Decoder(subBuffer);
                        Option option = description.produce();
                        option.decode(subDecoder);
                        result.add(option);
                    }
                    break;
                }
            }
        }
        return result;
    }

    public Option findOption(OptionDescription description) {
        for (Option option : options) {
            if (option.getDescription() == description) {
                return option;
            }
        }
        return null;
    }

    public void decode(Decoder decoder) {
        opcode = decoder.getByte();
        hardwareAddressType = decoder.getByte();
        hardwareAddressLength = decoder.getByte();
        hops = decoder.getByte();
        transactionId = decoder.getInt();
        elapsedSeconds = decoder.getShort() & 0xFFFF;
        flags = decoder.getShort();
        clientIpAddress = decoder.getIPAddress();
        yourIpAddress = decoder.getIPAddress();
        nextServerIpAddress = decoder.getIPAddress();
        relayAgentIpAddress = decoder.getIPAddress();
        decoder.getBytes(clientHardwareAddress);
        final byte[] bytes = decoder.getBytes(SERVER_HOSTNAME_SIZE + BOOT_FILE_SIZE);
        final int magic = decoder.getInt();
        if (magic != OPTIONS_MAGIC)
            throw new DHCPIllegalFormatException("Invalid options magic number: " + magic);
        options.addAll(decodeOptions(decoder));
        Option optionOverload = findOption(OptionOverloadOptionDescription.INSTANCE);
        if (optionOverload != null) {
            OptionOverloadOption optionOverloadOption = (OptionOverloadOption) optionOverload;
            ByteBuffer buffer;
            switch (optionOverloadOption.getCode()) {
                case OnlyFile: {
                    serverHostname = getString(bytes, 0, SERVER_HOSTNAME_SIZE);
                    buffer = ByteBuffer.wrap(bytes, SERVER_HOSTNAME_SIZE, BOOT_FILE_SIZE);
                    break;
                }
                case OnlyServerName: {
                    bootFile = getString(bytes, SERVER_HOSTNAME_SIZE, BOOT_FILE_SIZE);
                    buffer = ByteBuffer.wrap(bytes, 0, SERVER_HOSTNAME_SIZE);
                    break;
                }
                case Both: {
                    buffer = ByteBuffer.wrap(bytes);
                    break;
                }
                default:
                    throw new IllegalStateException();
            }
            Decoder additional = new Decoder(buffer);
            options.addAll(decodeOptions(additional));
        } else {
            serverHostname = getString(bytes, 0, SERVER_HOSTNAME_SIZE);
            bootFile = getString(bytes, SERVER_HOSTNAME_SIZE, BOOT_FILE_SIZE);
        }

        for (Option option : options) {
            if (option instanceof BootFileNameOption) {
                bootFile = ((BootFileNameOption) option).getName();
            }
        }
    }

    private static final byte[] EMPTY_BYTES = new byte[0];

    private void putString(byte[] data, int start, int max, String value) {
        byte[] bytes = value == null ? EMPTY_BYTES : value.getBytes(StandardCharsets.US_ASCII);
        int size = Math.min(bytes.length, max);
        System.arraycopy(bytes, 0, data, start, size);
        Arrays.fill(data, start + size, start + max, (byte) 0);
    }

    private int encodeOptions(final Encoder encoder, final List<Option> options, final int start) {
        byte[] bytes = new byte[256];
        ByteBuffer optionBuffer = ByteBuffer.wrap(bytes);
        Encoder optionEncoder = new Encoder(optionBuffer);
        int count = start;
        for (final Iterator<Option> iterator = options.stream().skip(start).iterator(); iterator.hasNext(); ++count) {
            Option option = iterator.next();
            optionBuffer.rewind();
            try {
                encoder.putByte(option.getDescription().getType());
                option.encode(optionEncoder);
                final int position = optionBuffer.position();
                encoder.putByte((byte) position);
                encoder.putBytes(bytes, 0, position);
            } catch (BufferOverflowException e) {
                return count;
            }
        }
        return count;
    }

    private static final int HEAD_AND_OPTS = HEAD_SIZE + 4 /*magic*/ + 1 /*end option*/;

    private boolean addBootFileOption(Encoder encoder) {
        String bootFile = this.bootFile;
        if (bootFile != null) {
            BootFileNameOption opt = new BootFileNameOption();
            opt.setName(bootFile);
            List<Option> addOpt = Collections.singletonList(opt);
            if (encodeOptions(encoder, addOpt, 0) != 1) {
                // failed to encode boot file option... abort
                log.warn("failed to encode boot file option in boot file option overload, what am I even doing?!");
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    private void encodeBoth(final byte[] overloadBytes, final List<Option> options, final int start) {
        // still not enough, use both
        log.warn("not enough space for all options; overload boot file and server name");
        final ByteBuffer bothBuffer =
                ByteBuffer.wrap(overloadBytes, 0, SERVER_HOSTNAME_SIZE + BOOT_FILE_SIZE - 1 /*end option*/);
        final Encoder bothEncoder = new Encoder(bothBuffer);
        final int offset = encodeOptions(bothEncoder, options, start);
        if (offset != options.size()) {
            log.warn("not enough space for all options; give up");
        } else {
            if (!addBootFileOption(bothEncoder)) {
                log.warn("not enough space for all options; only boot file name option left behind");
            }
        }
        overloadBytes[bothBuffer.position()] = END_OPTION;
    }

    public void encode(final Encoder encoder) {
        final int maxSize = encoder.size();
        if (maxSize < HEAD_AND_OPTS)
            throw new IllegalArgumentException("maxSize must be at least " + HEAD_AND_OPTS + ", got " + maxSize);
        encoder.putByte(opcode);
        encoder.putByte(hardwareAddressType);
        encoder.putByte(hardwareAddressLength);
        encoder.putByte(hops);
        encoder.putInt(transactionId);
        if (elapsedSeconds < 0 || elapsedSeconds >= 0xFFFF)
            throw new IllegalStateException("elapsed seconds value does not fit in 16 bit integer");
        encoder.putShort((short) elapsedSeconds);
        encoder.putShort(flags);
        encoder.putIPAddress(clientIpAddress);
        encoder.putIPAddress(yourIpAddress);
        encoder.putIPAddress(nextServerIpAddress);
        encoder.putIPAddress(relayAgentIpAddress);
        encoder.putBytes(clientHardwareAddress);
        // Options
        final int availableSpace = maxSize - HEAD_AND_OPTS - 3 /*option overload*/;
        final byte[] overloadBytes = new byte[SERVER_HOSTNAME_SIZE + BOOT_FILE_SIZE];
        if (availableSpace < 0) {
            // no options
            log.warn("not enough space for any option, do not know what to do");
            putString(overloadBytes, 0, SERVER_HOSTNAME_SIZE, serverHostname);
            putString(overloadBytes, SERVER_HOSTNAME_SIZE, BOOT_FILE_SIZE, bootFile);
            encoder.putBytes(overloadBytes);
            encoder.putInt(OPTIONS_MAGIC);
            encoder.putByte(END_OPTION);
            return;
        }
        final byte[] bytes = new byte[availableSpace];
        final ByteBuffer buffer = ByteBuffer.wrap(bytes);
        final Encoder optionsEncoder = new Encoder(buffer);
        final int firstOffset = encodeOptions(optionsEncoder, options, 0);
        final OptionOverloadOption.Variants variant;
        if (firstOffset != options.size()) {
            log.warn("not enough space for all options; overload boot file");
            // try boot file overload
            final ByteBuffer bootFileBuffer =
                    ByteBuffer.wrap(overloadBytes, SERVER_HOSTNAME_SIZE, BOOT_FILE_SIZE - 1 /*end option*/);
            final Encoder bootFileEncoder = new Encoder(bootFileBuffer);
            final int bootOffset = encodeOptions(bootFileEncoder, options, firstOffset);
            if (bootOffset == options.size()) {
                // enough, encode overload and add boot file option if required
                if (addBootFileOption(bootFileEncoder)) {
                    // space for server hostname exists, use it
                    overloadBytes[SERVER_HOSTNAME_SIZE + bootFileBuffer.position()] = END_OPTION; // add end option
                    putString(overloadBytes, 0, SERVER_HOSTNAME_SIZE, serverHostname);
                    variant = OptionOverloadOption.Variants.OnlyFile;
                } else {
                    // failed to encode this single option
                    encodeBoth(overloadBytes, options, firstOffset);
                    variant = OptionOverloadOption.Variants.Both;
                }
            } else {
                // no space at all, use maximum overload
                encodeBoth(overloadBytes, options, firstOffset);
                variant = OptionOverloadOption.Variants.Both;
            }
        } else {
            // enough space for everything, encode server name and boot file name
            putString(overloadBytes, 0, SERVER_HOSTNAME_SIZE, serverHostname);
            putString(overloadBytes, SERVER_HOSTNAME_SIZE, BOOT_FILE_SIZE, bootFile);
            variant = null;
        }
        // Encode overload option if required
        if (variant != null) {
            OptionOverloadOption option = new OptionOverloadOption();
            option.setCode(variant);
            option.encode(optionsEncoder);
        }
        // Dump result
        encoder.putBytes(overloadBytes);
        encoder.putInt(OPTIONS_MAGIC);
        final int optionsSize = buffer.position();
        encoder.putBytes(bytes, 0, optionsSize);
        encoder.putByte(END_OPTION);
    }

    public ClientIdentifier getClientIdentifier() {
        Option option = findOption(ClientIdentifierOptionDescription.INSTANCE);
        if (option != null) {
            ClientIdentifierOption identifierOption = (ClientIdentifierOption) option;
            return identifierOption.getIdentifier();
        } else {
            return ClientIdentifier.get(
                    hardwareAddressType, clientHardwareAddress, 0, Byte.toUnsignedInt(hardwareAddressLength)
            );
        }
    }

    public void setClientIdentifier(final ClientIdentifier identifier) {
        final int size = identifier.getSize();
        if (size > CLIENT_IDENTIFIER_FIELD_SIZE) {
            ClientIdentifierOption option = new ClientIdentifierOption();
            option.setIdentifier(identifier);
            addOption(option);
        } else {
            hardwareAddressType = identifier.getType();
            hardwareAddressLength = (byte) identifier.getSize();
            identifier.put(clientHardwareAddress, 0);
        }
    }

    public int getServerIdentifier() {
        final Option option = findOption(ServerIdentifierOptionDescription.INSTANCE);
        if (option == null) {
            log.warn("server identifier not specified");
            return 0;
        } else {
            return ((ServerIdentifierOption) option).getAddress();
        }
    }

    public DHCPMessageTypeOption.Types getMessageType() {
        final Option option = findOption(DHCPMessageTypeOptionDescription.INSTANCE);
        if (option == null) {
            throw new IllegalStateException("No DHCP message type option in DHCP Packet");
        }
        return ((DHCPMessageTypeOption) option).getType();
    }

    public void setMessageType(final DHCPMessageTypeOption.Types type) {
        final DHCPMessageTypeOption option = new DHCPMessageTypeOption();
        option.setType(type);
        options.add(option);
    }

    public int getPreferredAddress() {
        final Option option = findOption(RequestedIpAddressOptionDescription.INSTANCE);
        if (option == null) {
            return 0;
        } else {
            return ((RequestedIpAddressOption) option).getAddress();
        }
    }

    public long getPreferredLease() {
        final Option option = findOption(IpAddressLeaseTimeOptionDescription.INSTANCE);
        if (option == null) {
            return 0;
        } else {
            return ((IpAddressLeaseTimeOption) option).getSeconds();
        }
    }

    public void setPreferredLease(final long seconds) {
        final IpAddressLeaseTimeOption option = new IpAddressLeaseTimeOption();
        option.setSeconds(seconds);
        addOption(option);
    }

    public byte[] getParameters() {
        final Option option = findOption(ParameterRequestListOptionDescription.INSTANCE);
        if (option == null) {
            return null;
        } else {
            return ((ParameterRequestListOption) option).getBytes();
        }
    }

    public int getMaximumResponseSize() {
        final Option option = findOption(MaximumDHCPMessageSizeOptionDescription.INSTANCE);
        if (option == null) {
            return PREFERRED_MAX_SIZE;
        } else {
            return ((MaximumDHCPMessageSizeOption) option).getSize();
        }
    }

    public void setErrorMessage(final String message) {
        final MessageOption option = new MessageOption();
        option.setName(message);
        addOption(option);
    }

    public void addOption(final Option option) {
        options.add(option);
    }

    public void addOptions(final Collection<Option> options) {
        this.options.addAll(options);
    }
}
