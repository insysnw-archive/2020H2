#!/bin/sh

if [ $# = 0 ]; then
    >&2 echo 'one argument required: "server" or "client"'
    exit 1
fi

name="$1"
shift

if [ "$name" = "server" ]; then
    lab1-chat-server "$@"
elif [ "$name" = "client" ]; then
    lab1-chat-client "$@"
else
    >&2 echo 'the first argument should be either "server" or "client"'
fi
