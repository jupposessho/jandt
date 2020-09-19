package jupposessho.model

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

final case class ErrorResponse(errors: List[String])

object ErrorResponse {
  implicit val errorEncoder: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
  implicit def errorEntityEncoder[F[_]]: EntityEncoder[F, ErrorResponse] =
    jsonEncoderOf[F, ErrorResponse]
}
