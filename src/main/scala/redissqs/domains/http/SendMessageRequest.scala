package redissqs.domains.http

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finatra.validation.NotEmpty

case class SendMessageRequest(@NotEmpty name: String, value: JsonNode)
