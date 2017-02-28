package redissqs.domains.http

import com.fasterxml.jackson.databind.JsonNode

case class DeleteMessageRequest(name: String, value: JsonNode)
