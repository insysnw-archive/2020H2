require 'socket'
include Socket::Constants

@clients = []

class String
  def numeric?
    !Float(self).nil?
  rescue StandardError
    false
  end
end

# @param [Socket] socket
# @return [nil]
def handler(socket)
  socket.puts "Hello dear user~@~"
  loop do
    text = ''
    until text.end_with? "~@~"
      text << socket.recvfrom(256)[0].chomp
    end
    if text.index('[') != 0
      puts stderr 'Incorrect format of message'
    else
      dt = Time.at(text[1..text.index(']')].to_i)
      text = "[#{dt.strftime('%I:%M%p')}] #{text[text.index(']')+2..-1]}"
      print text[0..-4] + "\n"
      @clients.each do |c|
        c.puts(text) if c != socket
      end
    end
  rescue Errno::ECONNRESET
    warn 'Client left as'
    @clients.delete_at(@clients.index(socket))
    socket.close
  rescue Errno::EPIPE
    @clients.delete_at(@clients.index(socket))
    socket.close
    exit
  rescue IOError
    exit
  end
end

def shut_down
  @clients.each do |c|
    c.puts "Server is out. Please reconnect~@~"
    c.close
    rescue Errno::EPIPE
      @clients.delete_at(@clients.index(c))
      c.close
      retry
  end
  @socket.close
end

if ARGV.empty?
  warn 'No port number in args'
  exit 1
end

unless ARGV[0].numeric?
  warn 'Port is not number'
  exit 1
end

@socket = Socket.new(AF_INET, SOCK_STREAM, 0)
sockaddr = Socket.pack_sockaddr_in(ARGV[0].to_i, 'localhost')
@socket.bind(sockaddr)
@socket.listen(5)
print "Server is online. Press Ctrl+C to exit\n"
Signal.trap('INT') do
  shut_down
  exit
end
loop do
  client, client_addrinfo = @socket.accept
  print 'New client on server ' + client_addrinfo.inspect_sockaddr + "\n"
  @clients << client
  Thread.new { handler(client) }
end
