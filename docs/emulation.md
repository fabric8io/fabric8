## Emulation Layer

This layer supports the Kubernetes REST APIs on non-linux platforms (as some folks using Java middleware don't use linux). The use case is specifically to just support running Java application servers on non-linux; we have no plans to try emulate the whole docker ecosystem.

The Emulation layer is implemented by the [Jube](https://github.com/jubeio/jube) open source project which is a pure Java implementation of Kubernetes.

This lets you develop all your applications as if everything is running on Kubernetes; even when you need to run Java middleware on operating systems that are not modern linux environments (e.g. AIX / Solaris) or when Go Lang or Docker are not natively supported.