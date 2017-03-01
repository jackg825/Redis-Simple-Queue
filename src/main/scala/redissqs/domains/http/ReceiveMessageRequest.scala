package redissqs.domains.http

import com.twitter.finatra.validation.NotEmpty

case class ReceiveMessageRequest(@NotEmpty name: String)
