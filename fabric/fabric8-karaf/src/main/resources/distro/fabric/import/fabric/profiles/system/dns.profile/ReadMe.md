Enables DNS support in your fabric so that you can lookup containers or services via DNS.

e.g. once this profile is running in a container you can lookup things via:

    dig -p 8053 mybroker.container.fabric8.local @localhost
or

    dig -p 8053 fabric-repo.service.fabric8.local SRV @localhost

