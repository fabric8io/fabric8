# fabric-security-sso-client

## Description

fabric-security-sso-client is a simple utility that uses the OpenAM REST API to provide authentication/authorization.

## Usage

The main way of using the rest client is to authenticate with a username and password, then authorize the returned token:

    OpenAMRestClient client = new OpenAMRestClient();
    String token = client.authenticate("myuser", "pass");
    if (client.authorize("http://localhost:80/some_resource", token)) {
        System.out.println("Logged in!");
    } else {
        System.out.println("Failed authorization!");
    }

This of course requires a working OpenAM server running somewhere --> https://wikis.forgerock.org/confluence/display/openam/OpenAM+Installation+Guide

## TODO
* Need to test with https, ideally with mutual authentication, adding whatever additional configuration for keystores/truststores is necessary.
* Can get user information via a token, might be good to add a method for that.


