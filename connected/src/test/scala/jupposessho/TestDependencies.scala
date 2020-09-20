package jupposessho

import zio.logging.slf4j.Slf4jLogger

object TestDependencies {

  def logger(name: String) = Slf4jLogger.make(
    logFormat = (_, logEntry) => logEntry,
    rootLoggerName = Some(name)
  )
}
