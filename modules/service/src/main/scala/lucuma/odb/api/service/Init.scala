// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.odb.api.service

import cats.Applicative
import cats.data.State
import lucuma.odb.api.model._
import lucuma.odb.api.model.SequenceModel._
import lucuma.odb.api.repo.OdbRepo
import lucuma.core.`enum`._
import lucuma.core.optics.syntax.all._
import lucuma.core.math.syntax.int._
import cats.effect.Sync
import cats.syntax.all._
import io.circe.parser.decode
import lucuma.core.model.Program
import lucuma.odb.api.model.OffsetModel.ComponentInput
import monocle.state.all._

import scala.concurrent.duration._

object Init {

  val targetsJson = List(
"""
{
  "name":  "NGC 5949",
  "ra":    { "hms":  "15:28:00.668" },
  "dec":   { "dms": "64:45:47.4"  },
  "epoch": "J2000.000",
  "properMotion": {
    "ra":  { "milliarcsecondsPerYear": 0.0 },
    "dec": { "milliarcsecondsPerYear": 0.0 }
  },
  "radialVelocity": { "metersPerSecond": 423607 },
  "parallax":       { "milliarcseconds":  0.00 },
  "magnitudes": [
    {
      "band": "B",
      "value": 12.7,
      "system": "VEGA"
    },
    {
      "band": "R",
      "value": 12.252,
      "system": "VEGA"
    },
    {
      "band": "J",
      "value": 10.279,
      "system": "VEGA"
    },
    {
      "band": "H",
      "value": 9.649,
      "system": "VEGA"
    },
    {
      "band": "K",
      "value": 9.425,
      "system": "VEGA"
    }
  ]
}
""",
"""
{
  "name":  "NGC 3269",
  "ra":    { "hms":  "10:29:57.070" },
  "dec":   { "dms": "-35:13:27.8"  },
  "epoch": "J2000.000",
  "properMotion": {
    "ra":  { "milliarcsecondsPerYear": 0.0 },
    "dec": { "milliarcsecondsPerYear": 0.0 }
  },
  "radialVelocity": { "metersPerSecond": 3753885 },
  "parallax":       { "milliarcseconds":  0.00 },
  "magnitudes": [
    {
      "band": "B",
      "value": 13.24,
      "system": "VEGA"
    },
    {
      "band": "V",
      "value": 13.51,
      "system": "VEGA"
    },
    {
      "band": "R",
      "value": 11.73,
      "system": "VEGA"
    },
    {
      "band": "J",
      "value": 9.958,
      "system": "VEGA"
    },
    {
      "band": "H",
      "value": 9.387,
      "system": "VEGA"
    },
    {
      "band": "K",
      "value": 9.055,
      "system": "VEGA"
    }
  ]
}
""",
"""
{
  "name":  "NGC 3312",
  "ra":    { "hms": "10:37:02.549" },
  "dec":   { "dms": "-27:33:54.17"  },
  "epoch": "J2000.000",
  "properMotion": {
    "ra":  { "milliarcsecondsPerYear": 0.0 },
    "dec": { "milliarcsecondsPerYear":  0.0 }
  },
  "radialVelocity": { "metersPerSecond": 2826483 },
  "parallax":       { "milliarcseconds":  0.0 },
  "magnitudes": [
    {
      "band": "B",
      "value": 12.63,
      "system": "VEGA"
    },
    {
      "band": "V",
      "value": 13.96,
      "system": "VEGA"
    },
    {
      "band": "J",
      "value": 9.552,
      "system": "VEGA"
    },
    {
      "band": "H",
      "value": 8.907,
      "system": "VEGA"
    },
    {
      "band": "K",
      "value": 8.665,
      "system": "VEGA"
    }
  ]
}
"""
  )

  val targets: Either[Exception, List[TargetModel.CreateSidereal]] =
    targetsJson.traverse(decode[TargetModel.CreateSidereal])

  import GmosModel.{CreateCcdReadout, CreateSouthDynamic}
  import StepModel.CreateStep

  import CreateSouthDynamic.{exposure, filter, fpu, grating, readout, roi, step}
  import CreateCcdReadout.{ampRead, xBin, yBin}

  private def edit[A](start: A)(state: State[A, _]): A =
    state.runS(start).value

  val gmosAc: CreateSouthDynamic =
    CreateSouthDynamic(
      FiniteDurationModel.Input(10.seconds),
      CreateCcdReadout(
        GmosXBinning.Two,
        GmosYBinning.Two,
        GmosAmpCount.Twelve,
        GmosAmpGain.Low,
        GmosAmpReadMode.Fast
      ),
      GmosDtax.Zero,
      GmosRoi.Ccd2,
      None,
      Some(GmosSouthFilter.RPrime),
      None
    )

  val ac1: CreateStep[CreateSouthDynamic] =
    CreateStep.science(gmosAc, OffsetModel.Input.Zero)

  val ac2: CreateStep[CreateSouthDynamic] =
    edit(ac1) {
      for {
        _ <- step.p                                         := ComponentInput(10.arcsec)
        _ <- step.exposure                                  := FiniteDurationModel.Input(20.seconds)
        _ <- (step.instrumentConfig ^|-> readout ^|-> xBin) := GmosXBinning.One
        _ <- (step.instrumentConfig ^|-> readout ^|-> yBin) := GmosYBinning.One
        _ <- (step.instrumentConfig ^|-> roi)               := GmosRoi.CentralStamp
        _ <- (step.instrumentConfig ^|-> fpu)               := GmosSouthFpu.LongSlit_1_00.asRight.some
      } yield ()
    }

  val ac3: CreateStep[CreateSouthDynamic] =
    step.exposure.assign_(FiniteDurationModel.Input(30.seconds)).runS(ac2).value

  val acquisitionSequence: List[Atom.Create[CreateSouthDynamic]] =
    List(ac1, ac2, ac3).map(Atom.Create.continueTo) ++
      List.fill(10)(Atom.Create.stopBefore(ac3))

  val gcal: GcalModel.Create =
    GcalModel.Create(
      GcalContinuum.QuartzHalogen.some,
      List.empty[GcalArc],
      GcalFilter.Gmos,
      GcalDiffuser.Visible,
      GcalShutter.Closed,
      FiniteDurationModel.Input.fromSeconds(3.0),
      CoAddsModel.Input(1)
    )

  val Q15: OffsetModel.Input =
    OffsetModel.Input.fromArcseconds(0.0, 15.0)

  val gmos520: CreateSouthDynamic =
    edit(gmosAc) {
      for {
        _ <- exposure               := FiniteDurationModel.Input.fromSeconds(950.0)
        _ <- (readout ^|-> ampRead) := GmosAmpReadMode.Slow
        _ <- (readout ^|-> xBin)    := GmosXBinning.Two
        _ <- (readout ^|-> yBin)    := GmosYBinning.Two
        _ <- roi                    := GmosRoi.CentralSpectrum
        _ <- grating                := GmosModel.CreateGrating[GmosSouthDisperser](GmosSouthDisperser.B600_G5323, GmosDisperserOrder.One, WavelengthModel.Input.fromNanometers(520.0)).some
        _ <- filter                 := Option.empty[GmosSouthFilter]
        _ <- fpu                    := GmosSouthFpu.LongSlit_1_00.asRight.some
      } yield ()
    }

  val gmos525: CreateSouthDynamic =
    edit(gmos520)(
      CreateSouthDynamic.instrument.wavelength := WavelengthModel.Input.fromNanometers(525.0)
    )

  val flat_520: CreateStep[CreateSouthDynamic] =
    CreateStep.gcal(gmos520, gcal)

  val flat_525: CreateStep[CreateSouthDynamic] =
    CreateStep.gcal(gmos525, gcal)

  val sci0_520: CreateStep[CreateSouthDynamic] =
    CreateStep.science(gmos520, OffsetModel.Input.Zero)

  val sci15_520: CreateStep[CreateSouthDynamic] =
    CreateStep.science(gmos520, Q15)

  val sci0_525: CreateStep[CreateSouthDynamic] =
    CreateStep.science(gmos525, OffsetModel.Input.Zero)

  val sci15_525: CreateStep[CreateSouthDynamic] =
    CreateStep.science(gmos525, Q15)

  val scienceSequence: List[Atom.Create[CreateSouthDynamic]] =
    List(
      flat_520,  sci0_520,
      sci15_520, flat_520,
      flat_520,  sci15_520,
      sci0_520,  flat_520,
      flat_520,  sci0_520,
      sci15_520, flat_520,

      flat_525,  sci15_525,
      sci0_525,  flat_525,
      flat_525,  sci0_525,
      sci15_525, flat_525
    ).map(BreakpointStep.Create.continueTo)
     .grouped(2) // pairs flat and science steps
     .toList
     .map(Atom.Create(_))

  def obs(
    pid:   Program.Id,
    target: Option[TargetModel]
  ): ObservationModel.Create =
    ObservationModel.Create(
      observationId = None,
      programId     = pid,
      name          = target.map(_.target.name.value) orElse Some("Observation"),
      asterismId    = None,
      targetId      = target.map(_.id),
      status        = Some(ObsStatus.New),
      config        = Some(
        ConfigModel.Create.gmosSouth(
          ConfigModel.CreateGmosSouth(
            Sequence.Create(
              GmosModel.CreateSouthStatic.Default,
              acquisitionSequence,
              scienceSequence
            )
          )
        )
      )
    )

  /**
   * Initializes a (presumably) empty ODB with some demo values.
   */
  def initialize[F[_]: Sync: Applicative](repo: OdbRepo[F]): F[Unit] =
    for {
      p  <- repo.program.unsafeInsert(
              ProgramModel.Create(
                None,
                Some("The real dark matter was the friends we made along the way")
              )
            )
      _  <- repo.program.unsafeInsert(
              ProgramModel.Create(
                None,
                Some("An Empty Placeholder Program")
              )
            )
      cs <- targets.liftTo[F]
      ts <- cs.map(_.copy(programIds = Some(List(p.id)))).traverse(repo.target.unsafeInsertSidereal)
//      a0 <- repo.asterism.insert(
//              AsterismModel.Create(
//                None,
//                Some("More Constellation Than Asterism"),
//                List(p.id),
//                None
//              )
//            )
//      _  <- repo.asterism.shareWithTargets(Sharing[Asterism.Id, Target.Id](a0.id, ts.take(2).map(_.id)))
      _  <- repo.observation.insert(obs(p.id, ts.headOption))
      _  <- repo.observation.insert(obs(p.id, ts.lastOption))
      _  <- repo.observation.insert(obs(p.id, None))
    } yield ()

}
