package cz.smarteon.loxone.example

import cz.smarteon.loxone.LoxoneClient
import cz.smarteon.loxone.LoxoneCredentials
import cz.smarteon.loxone.LoxoneEndpoint
import cz.smarteon.loxone.LoxoneProfile
import cz.smarteon.loxone.ktor.HttpLoxoneClient
import cz.smarteon.loxone.message.LoxoneMessage


suspend fun main(args: Array<String>) {
    val loxoneClient: LoxoneClient = HttpLoxoneClient(
        LoxoneProfile(
            LoxoneEndpoint(args[0], useSsl = false),
            LoxoneCredentials(args[1], args[2])
        )
    )

    println(loxoneClient.callRaw("/jdev/cfg/api"))
    println(loxoneClient.call("/jdev/cfg/api", LoxoneMessage::class))
}
