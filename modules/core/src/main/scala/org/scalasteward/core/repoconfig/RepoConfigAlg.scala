/*
 * Copyright 2018-2019 scala-steward contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.scalasteward.core.repoconfig

import cats.data.OptionT
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.circe.config.parser
import org.scalasteward.core.github.data.Repo
import org.scalasteward.core.io.{FileAlg, WorkspaceAlg}
import org.scalasteward.core.util.MonadThrowable

class RepoConfigAlg[F[_]](
    implicit
    fileAlg: FileAlg[F],
    logger: Logger[F],
    workspaceAlg: WorkspaceAlg[F],
    F: MonadThrowable[F]
) {
  def getRepoConfig(repo: Repo): F[RepoConfig] =
    workspaceAlg.repoDir(repo).flatMap { dir =>
      val file = dir / ".scala-steward.conf"
      val maybeRepoConfig = OptionT(fileAlg.readFile(file)).flatMapF { content =>
        parser.decode[RepoConfig](content) match {
          case Right(config) =>
            F.pure(config.some)
          case Left(error) =>
            logger.info(s"Failed to parse ${file.name}: ${error.getMessage}").as(none[RepoConfig])
        }
      }
      maybeRepoConfig.getOrElse(RepoConfig())
    }
}
