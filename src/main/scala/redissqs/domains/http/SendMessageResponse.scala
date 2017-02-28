package redissqs.domains.http

import com.fasterxml.jackson.databind.JsonNode

case class SendMessageResponse(name: String, value: JsonNode)
