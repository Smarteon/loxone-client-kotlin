package cz.smarteon.loxone.example

import cz.smarteon.loxone.LoxoneClient
import cz.smarteon.loxone.LoxoneCredentials
import cz.smarteon.loxone.LoxoneEndpoint
import cz.smarteon.loxone.LoxoneProfile
import cz.smarteon.loxone.LoxoneTokenAuthenticator
import cz.smarteon.loxone.callForMsg
import cz.smarteon.loxone.ktor.HttpLoxoneClient
import cz.smarteon.loxone.message.ApiInfo


suspend fun main(args: Array<String>) {
    val endpoint = LoxoneEndpoint.fromUrl(args[0])
    val loxoneClient: LoxoneClient = HttpLoxoneClient(
        endpoint,
        LoxoneTokenAuthenticator(
            LoxoneProfile(
                endpoint,
                LoxoneCredentials(args[1], args[2])
            )
        )
    )

    println(loxoneClient.callRaw("jdev/cfg/api"))
    println(loxoneClient.call(ApiInfo.command))
    println(loxoneClient.callForMsg(ApiInfo.command))

    println(
        loxoneClient.callRaw("jdev/sps/LoxAPPversion3")
    )

    loxoneClient.close()
}
