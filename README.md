# Play Remote Configuration - HashiCorp Consul


[![Latest release](https://img.shields.io/badge/latest_release-18.12-orange.svg)](https://github.com/play-rconf/play-rconf-consul/releases)
[![JitPack](https://img.shields.io/badge/JitPack-release~18.12-brightgreen.svg)](https://jitpack.io/#play-rconf/play-rconf-consul)
[![Build](https://api.travis-ci.org/play-rconf/play-rconf-consul.svg?branch=master)](https://travis-ci.org/play-rconf/play-rconf-consul)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/play-rconf/play-rconf-consul/master/LICENSE)

Retrieves configuration from HashiCorp Consul
*****

## About this project
In production, it is not always easy to manage the configuration files of a
Play Framework application, especially when it running on multiple servers.
The purpose of this project is to provide a simple way to use a remote
configuration with a Play Framework application.



## How to use

To enable this provider, just add the classpath `"io.playrconf.provider.ConsulProvider"`
and the following configuration:

```hocon
remote-configuration {

  ## HashiCorp Consul
  # ~~~~~
  # Retrieves configuration from HashiCorp Consul
  consul {

    # API endpoint. HTTPS endpoint could be used,
    # but the SSL certificate must be valid
    endpoint = "http://127.0.0.1:8500/"
    endpoint = ${?REMOTECONF_CONSUL_ENDPOINT}

    # Authentication token. If ACL are anabled on
    # your Consul cluster, this variable allow you
    # to set the token to use with each API calls
    auth-token = ""
    auth-token = ${?REMOTECONF_CONSUL_AUTHTOKEN}

    # Prefix. Get only values with key beginning
    # with the configured prefix
    prefix = "/"
    prefix = ${?REMOTECONF_CONSUL_PREFIX}
  }
}
```



## License
This project is released under terms of the [MIT license](https://raw.githubusercontent.com/play-rconf/play-rconf-consul/master/LICENSE).
