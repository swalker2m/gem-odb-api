// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.odb.api.repo

import lucuma.odb.api.model.ProgramModel
import lucuma.odb.api.model.ProgramModel.ProgramEvent
import lucuma.odb.api.model.Existence._
import lucuma.odb.api.model.syntax.validatedinput._
import lucuma.core.model.{Asterism, Program, Target}
import cats.Monad
import cats.implicits._
import cats.MonadError
import cats.effect.concurrent.Ref


trait ProgramRepo[F[_]] extends TopLevelRepo[F, Program.Id, ProgramModel] {

  def selectAllForAsterism(aid: Asterism.Id, includeDeleted: Boolean = false): F[List[ProgramModel]]

  def selectAllForTarget(tid: Target.Id, includeDeleted: Boolean = false): F[List[ProgramModel]]

  def insert(input: ProgramModel.Create): F[ProgramModel]

}

object ProgramRepo {

  def create[F[_]: Monad](
    tablesRef:    Ref[F, Tables],
    eventService: EventService[F]
  )(implicit M: MonadError[F, Throwable]): ProgramRepo[F] =

    new TopLevelRepoBase[F, Program.Id, ProgramModel](
      tablesRef,
      eventService,
      Tables.lastProgramId,
      Tables.programs,
      ProgramEvent.apply
    ) with ProgramRepo[F]
      with LookupSupport {

      private def selectAllFor[I](
        id:             I,
        f:              Tables => ManyToMany[Program.Id, I],
        includeDeleted: Boolean
      ): F[List[ProgramModel]] =
        tablesRef.get.flatMap { tables =>
          f(tables).selectLeft(id).toList.traverse { pid =>
            tryFindProgram(tables, pid).liftTo[F]
          }
        }.map(deletionFilter(includeDeleted))

      override def selectAllForAsterism(aid: Asterism.Id, includeDeleted: Boolean = false): F[List[ProgramModel]] =
        selectAllFor(aid, _.programAsterism, includeDeleted)

      override def selectAllForTarget(tid: Target.Id, includeDeleted: Boolean = false): F[List[ProgramModel]] =
        selectAllFor(tid, _.programTarget, includeDeleted)

      override def insert(input: ProgramModel.Create): F[ProgramModel] =
        constructAndPublish { t =>
          tryNotFindProgram(t, input.programId).as(
            createAndInsert(input.programId, ProgramModel(_, Present, input.name))
          )
        }

    }

}