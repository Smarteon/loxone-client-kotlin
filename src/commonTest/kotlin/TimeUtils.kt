package cz.smarteon.loxone

import kotlinx.datetime.Clock

object TimeUtils {
    fun currentLoxoneSeconds() = Clock.System.now().epochSeconds - LoxoneTime.LOXONE_EPOCH_BEGIN
}
