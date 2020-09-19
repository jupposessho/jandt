package jupposessho.model

sealed trait AppError

object AppError {
  final case class UserNotFound(name: String) extends AppError
  final case class TwitterError(message: String) extends AppError
}
