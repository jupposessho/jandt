package jupposessho.model.github

import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe._
import zio.Task
import zio.interop.catz._

final case class Organization(id: Int, login: String, url: String)

object Organization {
  implicit val organizationDecoder: EntityDecoder[Task, List[Organization]] = jsonOf[Task, List[Organization]]
}
