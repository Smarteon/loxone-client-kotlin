package cz.smarteon.loxone.example;

import cz.smarteon.loxone.LoxoneClient;
import cz.smarteon.loxone.LoxoneCredentials;
import cz.smarteon.loxone.LoxoneEndpoint;
import cz.smarteon.loxone.LoxoneProfile;
import cz.smarteon.loxone.LoxoneTokenAuthenticator;
import cz.smarteon.loxone.ktor.HttpLoxoneClient;
import cz.smarteon.loxone.message.ApiInfo;

public class LoxoneClientExample {

    public static void main(String[] args) {
        System.out.println("Test");

        final var endpoint = new LoxoneEndpoint(args[0], 443);
        final LoxoneClient loxoneClient = new HttpLoxoneClient(
                endpoint,
                new LoxoneTokenAuthenticator(
                        new LoxoneProfile(
                                endpoint,
                                new LoxoneCredentials(args[1], args[2])
                        )
                )
        );

        final var client = new BlockingHttpLoxoneClient(loxoneClient);

        System.out.println(client.callRaw("/jdev/cfg/api"));
        System.out.println(client.call(ApiInfo.Companion.getCommand()));
        client.close();
    }
}
