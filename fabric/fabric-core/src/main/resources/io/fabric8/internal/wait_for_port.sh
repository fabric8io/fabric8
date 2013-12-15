function wait_for_port() {
    PORT=$1
    for i in {1..5};
        do
            echo -ne "\035" | telnet 127.0.0.1 $PORT > /dev/null 2>&1;
            [ $? -eq 1 ] && sleep 5;
        done
        return 0
}