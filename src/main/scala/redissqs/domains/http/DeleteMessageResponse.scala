package redissqs.domains.http

import com.fasterxml.jackson.databind.JsonNode

case class DeleteMessageResponse(name: String, value: JsonNode)
