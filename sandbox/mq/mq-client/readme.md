# Synopsis

The `mq-client` is simple producer or consumer for an ActiveMQ broker that can be used to validate an installation

Usage:

    java -jar lib/mq-client.jar producer [OPTIONS]
or
    java -jar lib/mq-client.jar consumer [OPTIONS]
    
where 

    options : [--destination (queue://..|topic://..) - ; default TEST
              [--persistent  true|false] - use persistent or non persistent messages; default true
              [--count       N] - number of messages to send or receive; default 100
              [--size        N] - size in bytes of a BytesMessage; default 0, a simple TextMessage is used
              [--textSize    N] - size in bytes of a TextMessage (supported values : 100b, 1K, 10K); default 100b, a Lorem ipsum dummy TextMessage is used
              [--sleep       N] - millisecond sleep period between sends or receives; default 0
              [--batchSize   N] - use send and receive transaction batches of size N; default 0, no jms transactions
              [--ttl         N] - message TTL in milliseconds
              [--parallelThreads N] - number of threads to run in parallel; default 1
              [--groupId  ..  ] - JMS message group identifier
              [--clientId   id] - use a durable topic consumer with the supplied id; default null, non durable consumer
              [--brokerUrl URL] - connection factory url; default " + ActiveMQConnectionFactory.DEFAULT_BROKER_URL);
              [--user      .. ] - connection user name
              [--password  .. ] - connection password    


