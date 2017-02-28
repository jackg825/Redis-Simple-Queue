package redissqs.utils

import com.twitter.io.Buf

object Implicits {

  implicit class StringOpt(v: String) {
    def toBuf = Buf.Utf8(v)
  }

  implicit class BufOpt(v: Buf) {
    def deBuf: Option[String] = Buf.Utf8.unapply(v)
  }
}
