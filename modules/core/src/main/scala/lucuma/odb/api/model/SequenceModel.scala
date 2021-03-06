// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.odb.api.model

import lucuma.core.util.Enumerated
import lucuma.odb.api.model.StepModel.CreateStep
import lucuma.odb.api.model.syntax.inputvalidator._

import cats.Eq
import cats.data.NonEmptyList
import cats.syntax.all._
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import monocle.{Iso, Lens}
import monocle.macros.Lenses

object SequenceModel {

  sealed trait Breakpoint extends Product with Serializable {

    def enabled: Boolean =
      this match {
        case Breakpoint.Enabled  => true
        case Breakpoint.Disabled => false
      }

  }

  object Breakpoint {

    case object Enabled  extends Breakpoint
    case object Disabled extends Breakpoint

    val enabled: Breakpoint =
      Enabled

    val disabled: Breakpoint =
      Disabled

    val fromBoolean: Iso[Boolean, Breakpoint] =
      Iso[Boolean, Breakpoint](b => if (b) Enabled else Disabled)(_.enabled)

    implicit val EnumeratedBreakpoint: Enumerated[Breakpoint] =
      Enumerated.of(enabled, disabled)

    implicit val DecoderBreakpoint: Decoder[Breakpoint] =
      deriveDecoder[Breakpoint]
  }


  @Lenses final case class BreakpointStep[A](
    breakpoint: Breakpoint,
    step:       StepModel[A]
  )

  object BreakpointStep {

    implicit def EqBreakpointStep[A: Eq]: Eq[BreakpointStep[A]] =
      Eq.by { a => (
        a.breakpoint,
        a.step
      )}

    @Lenses final case class Create[A](
      breakpoint: Breakpoint,
      step:       CreateStep[A]
    ) {

      def create[B](implicit V: InputValidator[A, B]): ValidatedInput[BreakpointStep[B]] =
        step.create[B].map(s => BreakpointStep(breakpoint, s))

    }

    object Create {

      def stopBefore[A](s: CreateStep[A]): Create[A] =
        Create(Breakpoint.enabled, s)

      def continueTo[A](s: CreateStep[A]): Create[A] =
        Create(Breakpoint.disabled, s)

      implicit def EqCreate[A: Eq]: Eq[Create[A]] =
        Eq.by { a => (
          a.breakpoint,
          a.step
        )}

      implicit def DecoderCreate[A: Decoder]: Decoder[Create[A]] =
        deriveDecoder[Create[A]]

      implicit def ValidatorCreate[A, B](implicit V: InputValidator[A, B]): InputValidator[Create[A], BreakpointStep[B]] =
        (cbs: Create[A]) => cbs.create[B]
    }

  }


  @Lenses final case class Atom[A](
    steps: NonEmptyList[BreakpointStep[A]]
  )


  object Atom {

    def one[A](head: BreakpointStep[A]): Atom[A] =
      Atom(NonEmptyList.one(head))

    def ofSteps[A](head: BreakpointStep[A], tail: BreakpointStep[A]*): Atom[A] =
      Atom(NonEmptyList.of(head, tail: _*))

    def fromNel[A]: Iso[NonEmptyList[BreakpointStep[A]], Atom[A]] =
      Iso[NonEmptyList[BreakpointStep[A]], Atom[A]](nel => Atom(nel))(_.steps)

    implicit def EqSequenceAtom[A: Eq]: Eq[Atom[A]] =
      Eq.by(_.steps)


    @Lenses final case class Create[A](
      steps: List[BreakpointStep.Create[A]]
    ) {

      def create[B](implicit V: InputValidator[A, B]): ValidatedInput[Atom[B]] =
        steps match {
          case Nil    =>
            InputError.fromMessage("Cannot create an empty sequence atom").invalidNec[Atom[B]]

          case h :: t =>
            (h.create[B], t.traverse(_.create[B])).mapN { (h0, t0) =>
              Atom.fromNel.get(NonEmptyList(h0, t0))
            }

        }
    }

    object Create {

      def singleton[A](step: BreakpointStep.Create[A]): Create[A] =
        Create(List(step))

      def stopBefore[A](step: CreateStep[A]): Create[A] =
        singleton(BreakpointStep.Create.stopBefore(step))

      def continueTo[A](step: CreateStep[A]): Create[A] =
        singleton(BreakpointStep.Create.continueTo(step))

      implicit def DecoderCreate[A: Decoder]: Decoder[Create[A]] =
        deriveDecoder[Create[A]]

      implicit def ValidatorCreate[A, B](implicit V: InputValidator[A, B]): InputValidator[Create[A], Atom[B]] =
        (csa: Create[A]) => csa.create[B]

      implicit def EqCreate[A: Eq]: Eq[Create[A]] =
        Eq.by(_.steps)

    }


  }

  /**
   * Sequence representation.
   *
   * @param static static configuration
   * @param acquisition acquisition steps
   * @param science science steps
   *
   * @tparam S static configuration type
   * @tparam D dynamic (step) configuration type
   */
  final case class Sequence[S, D](
    static:      S,
    acquisition: List[Atom[D]],
    science:     List[Atom[D]]
  )

  object Sequence extends SequenceOptics {

    implicit def EqSequence[S: Eq, D: Eq]: Eq[Sequence[S, D]] =
      Eq.by { a => (
        a.static,
        a.acquisition,
        a.science
      )}

    /**
     * Input for sequence creation.
     */
    final case class Create[CS, CD](
      static:      CS,
      acquisition: List[Atom.Create[CD]],
      science:     List[Atom.Create[CD]]
    ) {

      def create[S, D](
        implicit ev1: InputValidator[CS, S], ev2: InputValidator[CD, D]
      ): ValidatedInput[Sequence[S, D]] =
        (
          static.validateAndCreate[S],
          acquisition.traverse(_.create),
          science.traverse(_.create)
        ).mapN { (st, aq, sc) => Sequence(st, aq, sc) }

    }

    object Create {

      implicit def EdCreate[S: Eq, D: Eq]: Eq[Create[S, D]] =
        Eq.by { a => (
          a.static,
          a.acquisition,
          a.science
        )}

      implicit def DecoderCreate[S: Decoder, D: Decoder]:Decoder[Create[S, D]] =
        deriveDecoder[Create[S, D]]

      implicit def ValidatorCreate[CS, S, CD, D](
        implicit ev1: InputValidator[CS, S], ev2: InputValidator[CD, D]
      ): InputValidator[Create[CS, CD], Sequence[S, D]] =
        InputValidator.by[Create[CS, CD], Sequence[S, D]](_.create)

    }

  }

  sealed trait SequenceOptics { this: Sequence.type =>

    def static[S, D]: Lens[Sequence[S, D], S] =
      Lens[Sequence[S, D], S](_.static)(a => _.copy(static = a))

    def acquisition[S, D]: Lens[Sequence[S, D], List[Atom[D]]] =
      Lens[Sequence[S, D], List[Atom[D]]](_.acquisition)(a => _.copy(acquisition = a))

    def science[S, D]: Lens[Sequence[S, D], List[Atom[D]]] =
      Lens[Sequence[S, D], List[Atom[D]]](_.science)(a => _.copy(science = a))

  }

}
