package com.kutoru.mikunotes.logic

class InvalidUrl(message: String? = null) : Exception(message)
class Unauthorized(message: String? = null) : Exception(message)
class BadRequest(message: String? = null) : Exception(message)
class ServerError(message: String? = null) : Exception(message)
