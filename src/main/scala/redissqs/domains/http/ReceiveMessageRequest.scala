package redissqs.domains.http

import com.twitter.finatra.request.QueryParam
import com.twitter.finatra.validation.NotEmpty

case class ReceiveMessageRequest(@QueryParam @NotEmpty name: String)
