#!/bin/bash

exec 3<>/dev/tcp/localhost/9000

echo -e "GET /auth/-/health/ready/ HTTP/1.1
host: localhost:9000
" >&3

timeout 1 cat <&3 | grep HTTP/1.1 | grep OK || exit 1