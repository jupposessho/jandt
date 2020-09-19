package jupposessho.model

sealed trait AppError

object AppError {
  final case class UserNotFound(name: User) extends AppError
  final case class TwitterError(message: String) extends AppError
  final case class GithubError(message: String) extends AppError
}
