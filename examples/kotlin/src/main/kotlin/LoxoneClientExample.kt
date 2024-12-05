package cz.smarteon.loxkt.example

import cz.smarteon.loxkt.LoxoneAuth
import cz.smarteon.loxkt.LoxoneClient
import cz.smarteon.loxkt.LoxoneEndpoint
import cz.smarteon.loxkt.callForMsg
import cz.smarteon.loxkt.ktor.KtorHttpLoxoneClient
import cz.smarteon.loxkt.message.ApiInfo


suspend fun main(args: Array<String>) {
    val endpoint = LoxoneEndpoint.fromUrl(args[0])
    val loxoneClient: LoxoneClient = KtorHttpLoxoneClient(
        endpoint,
        LoxoneAuth.Basic(args[1], args[2])
    )

    println(loxoneClient.callRaw("jdev/cfg/api"))
    println(loxoneClient.call(ApiInfo.command))
    println(loxoneClient.callForMsg(ApiInfo.command))

    println(
        loxoneClient.callRaw("jdev/sps/LoxAPPversion3")
    )

    loxoneClient.close()
}
