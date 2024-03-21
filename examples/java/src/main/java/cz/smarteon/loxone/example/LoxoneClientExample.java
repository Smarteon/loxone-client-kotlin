package cz.smarteon.loxone.example;

import cz.smarteon.loxone.LoxoneAuth;
import cz.smarteon.loxone.LoxoneClient;
import cz.smarteon.loxone.LoxoneEndpoint;
import cz.smarteon.loxone.ktor.KtorHttpLoxoneClient;
import cz.smarteon.loxone.message.ApiInfo;

public class LoxoneClientExample {

    public static void main(String[] args) {
        System.out.println("Test");

        final var endpoint = new LoxoneEndpoint(args[0], 443);
        final LoxoneClient loxoneClient = new KtorHttpLoxoneClient(
                endpoint,
                new LoxoneAuth.Basic(args[1], args[2])
        );

        final var client = new BlockingHttpLoxoneClient(loxoneClient);

        System.out.println(client.callRaw("/jdev/cfg/api"));
        System.out.println(client.call(ApiInfo.Companion.getCommand()));
        client.close();
    }
}
