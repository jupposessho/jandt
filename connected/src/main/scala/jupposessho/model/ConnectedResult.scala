package jupposessho.model

import io.circe.{Encoder, Printer}
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderWithPrinterOf

final case class ConnectedResult(connected: Boolean, organisations: Option[List[String]])

object ConnectedResult {

  implicit val resultEncoder: Encoder[ConnectedResult] = deriveEncoder[ConnectedResult]
  implicit def resultEntityEncoder[F[_]]: EntityEncoder[F, ConnectedResult] =
    jsonEncoderWithPrinterOf[F, ConnectedResult](Printer.noSpaces.copy(dropNullValues = true))
}
