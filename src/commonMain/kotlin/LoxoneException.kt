package cz.smarteon.loxkt

/**
 * @property code the Miniserver's answer code when it refused a command, e.g. `412` for rights the user may not
 * have.
 */
class LoxoneException(message: String, cause: Throwable? = null, val code: String? = null) : Exception(message, cause)
