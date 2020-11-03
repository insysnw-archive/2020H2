require 'socket'
include Socket::Constants

class String
  def numeric?
    !Float(self).nil?
  rescue StandardError
    false
  end
end

print "Hello, client is starting\n"

if ARGV.length < 2
  warn "No server`s ip and port number in args\n"
  exit 1
end

if (ARGV[0] =~ /^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/).nil?
  warn "Server`s ip is not correct\n"
  exit 1
end

unless ARGV[1].numeric?
  warn "Port is not number\n"
  exit 1
end

socket = Socket.new(AF_INET, SOCK_STREAM, 0)
sockaddr = Socket.pack_sockaddr_in(ARGV[1].to_i, ARGV[0])
socket.connect(sockaddr)
loop do
  socket.puts '[123456879] 123456789'
  print socket.recvfrom(256)[0].chomp
  sleep 3
end
socket.close
