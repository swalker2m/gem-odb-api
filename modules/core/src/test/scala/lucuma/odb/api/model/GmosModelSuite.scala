// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.odb.api.model

import lucuma.odb.api.model.arb._
import lucuma.core.`enum`.GmosNorthDisperser
import lucuma.core.util.arb.ArbEnumerated
import cats.kernel.laws.discipline.EqTests
import munit.DisciplineSuite

final class GmosModelSuite extends DisciplineSuite {

  import ArbEnumerated._
  import ArbGmosModel._

  checkAll("GmosModel.NodAndShuffle",               EqTests[GmosModel.NodAndShuffle].eqv)
  checkAll("GmosModel.CreateNodAndShuffle",         EqTests[GmosModel.CreateNodAndShuffle].eqv)
  checkAll("GmosModel.EditNodAndShuffle",           EqTests[GmosModel.EditNodAndShuffle].eqv)


  checkAll("GmosModel.NorthStatic",                 EqTests[GmosModel.NorthStatic].eqv)
  checkAll("GmosModel.CreateNorthStatic",           EqTests[GmosModel.CreateNorthStatic].eqv)

  checkAll("GmosModel.SouthStatic",                 EqTests[GmosModel.SouthStatic].eqv)
  checkAll("GmosModel.CreateSouthStatic",           EqTests[GmosModel.CreateSouthStatic].eqv)

  checkAll("GmosModel.CcdReadout",                  EqTests[GmosModel.CcdReadout].eqv)
  checkAll("GmosModel.CreateCcdReadout",            EqTests[GmosModel.CreateCcdReadout].eqv)


  checkAll("GmosModel.CustomMask",                  EqTests[GmosModel.CustomMask].eqv)
  checkAll("GmosModel.CreateCustomMask",            EqTests[GmosModel.CreateCustomMask].eqv)

  checkAll("GmosModel.Grating",                     EqTests[GmosModel.Grating[GmosNorthDisperser]].eqv)
  checkAll("GmosModel.CreateGrating",               EqTests[GmosModel.CreateGrating[GmosNorthDisperser]].eqv)

  checkAll("GmosModel.NorthDynamic",                EqTests[GmosModel.NorthDynamic].eqv)
  checkAll("GmosModel.CreateNorthDynamic",          EqTests[GmosModel.CreateNorthDynamic].eqv)

  checkAll("GmosModel.SouthDynamic",                EqTests[GmosModel.SouthDynamic].eqv)
  checkAll("GmosModel.CreateSouthDynamic",          EqTests[GmosModel.CreateSouthDynamic].eqv)

}
