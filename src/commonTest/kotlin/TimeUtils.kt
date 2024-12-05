package cz.smarteon.loxkt

import kotlinx.datetime.Clock

object TimeUtils {
    fun currentLoxoneSeconds() = Clock.System.now().epochSeconds - LoxoneTime.LOXONE_EPOCH_BEGIN
}
