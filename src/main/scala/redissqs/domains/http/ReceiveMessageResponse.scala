package redissqs.domains.http

import com.fasterxml.jackson.databind.JsonNode

case class ReceiveMessageResponse(value: JsonNode)
