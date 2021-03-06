// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.odb.api.model
package arb

import lucuma.odb.api.model.SequenceModel._
import org.scalacheck._
import org.scalacheck.Arbitrary.arbitrary

trait ArbConfigModel {

  import ArbGmosModel._
  import ArbSequenceModel._

  implicit val arbGmosNorth: Arbitrary[ConfigModel.GmosNorth] =
    Arbitrary {
      arbitrary[Sequence[GmosModel.NorthStatic, GmosModel.NorthDynamic]]
        .map(ConfigModel.GmosNorth(_))
    }

  implicit val arbGmosSouth: Arbitrary[ConfigModel.GmosSouth] =
    Arbitrary {
      arbitrary[Sequence[GmosModel.SouthStatic, GmosModel.SouthDynamic]]
        .map(ConfigModel.GmosSouth(_))
    }

  implicit val cogGmosNorth: Cogen[ConfigModel.GmosNorth] =
    Cogen[Sequence[GmosModel.NorthStatic, GmosModel.NorthDynamic]]
      .contramap(_.manual)

  implicit val cogGmosSouth: Cogen[ConfigModel.GmosSouth] =
    Cogen[Sequence[GmosModel.SouthStatic, GmosModel.SouthDynamic]]
      .contramap(_.manual)

  implicit val arbCreateGmosNorth: Arbitrary[ConfigModel.CreateGmosNorth] =
    Arbitrary {
      arbitrary[Sequence.Create[GmosModel.CreateNorthStatic, GmosModel.CreateNorthDynamic]]
        .map(ConfigModel.CreateGmosNorth(_))
    }

  implicit val arbCreateGmosSouth: Arbitrary[ConfigModel.CreateGmosSouth] =
    Arbitrary {
      arbitrary[Sequence.Create[GmosModel.CreateSouthStatic, GmosModel.CreateSouthDynamic]]
        .map(ConfigModel.CreateGmosSouth(_))
    }

  implicit val cogCreateGmosNorth: Cogen[ConfigModel.CreateGmosNorth] =
    Cogen[Sequence.Create[GmosModel.CreateNorthStatic, GmosModel.CreateNorthDynamic]]
      .contramap(_.manual)

  implicit val cogCreateGmosSouth: Cogen[ConfigModel.CreateGmosSouth] =
    Cogen[Sequence.Create[GmosModel.CreateSouthStatic, GmosModel.CreateSouthDynamic]]
      .contramap(_.manual)

  implicit val arbConfigModel: Arbitrary[ConfigModel] =
    Arbitrary {
      Gen.oneOf(
        arbitrary[ConfigModel.GmosNorth],
        arbitrary[ConfigModel.GmosSouth]
      )
    }

  implicit val cogConfigModel: Cogen[ConfigModel] =
    Cogen[(
      Option[ConfigModel.GmosNorth],
      Option[ConfigModel.GmosSouth]
    )].contramap { in => (
      in.gmosNorth,
      in.gmosSouth
    )}

  implicit val arbConfigModelCreate: Arbitrary[ConfigModel.Create] =
    Arbitrary {
      Gen.oneOf(
        arbitrary[ConfigModel.CreateGmosNorth].map(ConfigModel.Create.gmosNorth),
        arbitrary[ConfigModel.CreateGmosSouth].map(ConfigModel.Create.gmosSouth)
      )
    }

  implicit val cogConfigModelCreate: Cogen[ConfigModel.Create] =
    Cogen[(
      Option[ConfigModel.CreateGmosNorth],
      Option[ConfigModel.CreateGmosSouth]
    )].contramap { in => (
      in.gmosNorth,
      in.gmosSouth
    )}

}

object ArbConfigModel extends ArbConfigModel

