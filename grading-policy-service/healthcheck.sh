#!/bin/bash

exec 3<>/dev/tcp/localhost/80

echo -e "GET /-/ready HTTP/1.1
host: localhost:80
" >&3

timeout 1 cat <&3 | grep HTTP/1.1 | grep OK || exit 1