package lab2.dhcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

public final class Repository {
    private static final Logger log = LoggerFactory.getLogger(Repository.class);

    private final Map<ClientIdentifier, Record> recordsByIdentifier = new HashMap<>();
    private final Map<Integer, Record> recordsByAddress = new HashMap<>();
    private int lastAddress;
    private final ServerConfiguration configuration;

    public Repository(final ServerConfiguration configuration) {
        this.configuration = configuration;
        lastAddress = configuration.getIpRangeStart();

        for (ServerConfiguration.HostConfiguration host : configuration.getHosts()) {
            final Record record = new Record(Types.Static, Instant.MAX, host.getIdentifier(), host.getAddress(), host);
            putRecord(record);
        }
    }

    private void removeRecord(Record record) {
        recordsByIdentifier.remove(record.getIdentifier());
        recordsByAddress.remove(record.getAddress());
    }

    private void putRecord(Record record) {
        recordsByIdentifier.put(record.getIdentifier(), record);
        recordsByAddress.put(record.getAddress(), record);
    }

    private boolean isFree(int address) {
        return recordsByAddress.get(address) == null;
    }

    private int getNextIPAddress() {
        final int start = configuration.getIpRangeStart();
        final int stop = configuration.getIpRangeStop();
        final int first = lastAddress;
        while (!isFree(++lastAddress)) {
            if (lastAddress == first)
                return 0;
            if (lastAddress > stop)
                lastAddress = start - 1;
        }
        return lastAddress;
    }

    private long getRealLease(long lease) {
        if (lease == 0) {
            return configuration.getLeaseTime();
        } else {
            return Math.min(lease, configuration.getMaxLeaseTime());
        }
    }

    public Response offer(final ClientIdentifier identifier, final int address, final long lease) {
        return offerInternal(identifier, address, getRealLease(lease), Types.Offered);
    }

    public Response occupy(final ClientIdentifier identifier, final long lease) {
        return offerInternal(identifier, 0, getRealLease(lease), Types.Occupied);
    }

    private Response offerInternal(final ClientIdentifier identifier, final int address, final long lease, final Types type) {
        final Record exists = recordsByIdentifier.get(identifier);
        if (exists != null) {
            // already assigned
            if (exists.expired()) {
                removeRecord(exists);
            } else {
                if (address != 0) {
                    if (exists.address == address) {
                        exists.updateLease(lease);
                        exists.updateType(type);
                        return new Response(exists, lease);
                    } else {
                        return new Response("different assigned");
                    }
                } else {
                    exists.updateLease(lease);
                    exists.updateType(type);
                    return new Response(exists, lease);
                }
            }
        }
        if (address != 0) {
            // Address proposed, use it
            Record occupied = recordsByAddress.get(address);
            if (occupied != null) {
                if (occupied.expired()) {
                    removeRecord(occupied);
                } else {
                    return new Response("occupied");
                }
            }
            if (address < configuration.getIpRangeStart() || address > configuration.getIpRangeStop()) {
                return new Response("network changed");
            }
            final Instant expire = (lease == 0xFFFF_FFFF /*infinity*/) ? Instant.MAX : Instant.now().plusSeconds(lease);
            final Record record = new Record(type, expire, identifier, address, null);
            putRecord(record);
            return new Response(record, lease);
        } else {
            // Create new address
            final int newAddress = getNextIPAddress();
            if (newAddress == 0) {
                log.warn("no addresses left");
                return new Response("exhausted");
            } else {
                final Instant expire = (lease == 0xFFFF_FFFF /*infinity*/) ?
                        Instant.MAX : Instant.now().plusSeconds(lease);
                Record record = new Record(type, expire, identifier, newAddress, null);
                putRecord(record);
                return new Response(record, lease);
            }
        }
    }

    public void decline(final ClientIdentifier identifier) {
        forget(identifier, Types.Offered);
    }

    public void release(final ClientIdentifier identifier) {
        forget(identifier, Types.Occupied);
    }

    private void forget(final ClientIdentifier identifier, final Types type) {
        final Record record = recordsByIdentifier.get(identifier);
        if (record == null) {
            log.warn("client tried remove not existing record of type {}", type);
        } else {
            if (record.type.ordinal() < type.ordinal()) {
                log.warn(
                        "client {} tried to remove record of different type (expected {}, got {})",
                        identifier, record.type, type
                );
            } else {
                removeRecord(record);
            }
        }
    }

    public void cleanup() {
        final Iterator<Map.Entry<ClientIdentifier, Record>> iterator = recordsByIdentifier.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<ClientIdentifier, Record> entry = iterator.next();
            final Record record = entry.getValue();
            if (record.expired()) {
                iterator.remove();
                recordsByAddress.remove(record.address);
            }
        }
    }

    public static final class Response {
        private final Record record;
        private final int lease;
        private final String message;

        public Response(Record record, long lease) {
            this.record = record;
            this.lease = (int) lease;
            this.message = null;
        }

        public Response(String message) {
            this.record = null;
            this.lease = 0;
            this.message = message;
        }

        public Record getRecord() {
            return record;
        }

        public long getLease() {
            return Integer.toUnsignedLong(lease);
        }

        public String getMessage() {
            return message;
        }

        public boolean isError() {
            return record == null;
        }
    }

    private enum Types {
        Offered, Occupied, Static
    }

    public final class Record implements OptionsKeeper {
        private Types type;
        private Instant expire;
        private final ClientIdentifier identifier;
        private final int address;
        private final ServerConfiguration.HostConfiguration host;

        public Record(
                Types type,
                Instant expire,
                ClientIdentifier identifier,
                int address,
                ServerConfiguration.HostConfiguration host
        ) {
            this.type = type;
            this.expire = expire;
            this.identifier = identifier;
            this.address = address;
            this.host = host;
        }

        private void updateLease(long lease) {
            final Instant updated = Instant.now().plusSeconds(lease);
            expire = (updated.compareTo(expire) > 0) ? updated : expire;
        }

        private void updateType(Types type) {
            if (type.ordinal() > this.type.ordinal())
                this.type = type;
        }

        public Types getType() {
            return type;
        }

        public boolean expired() {
            return expire.compareTo(Instant.now()) < 0;
        }

        public ClientIdentifier getIdentifier() {
            return identifier;
        }

        public int getAddress() {
            return address;
        }

        public List<Option> getOptions() {
            final List<Option> options = new ArrayList<>();
            if (host != null) {
                options.addAll(host.getOptions().values());
            }
            options.addAll(configuration.getDefaultOptions().values());
            return options;
        }

        public Option findOption(byte type) {
            if (host != null) {
                Option option = host.getOptions().get(type);
                if (option != null) {
                    return option;
                }
            }
            return configuration.getDefaultOptions().get(type);
        }
    }
}
