function find_free_port() {

   START_PORT=$1
   END_PORT=$2
   for port in `eval echo {$START_PORT..$END_PORT}`;do
    echo -ne "\035" | telnet 127.0.0.1 $port > /dev/null 2>&1;
     [ $? -eq 1 ] && echo $port && break;
   done
}