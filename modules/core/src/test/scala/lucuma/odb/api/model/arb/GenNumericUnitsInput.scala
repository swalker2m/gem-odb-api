// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.odb.api.model
package arb

import org.scalacheck.{Cogen, Gen}

trait GenNumericUnitsInput {

  import NumericUnits.{DecimalInput, LongInput}

  def genLongInput[A: Numeric, U](g: Gen[A], u: U): Gen[LongInput[U]] =
    g.map(a => LongInput[U](Numeric[A].toLong(a), u))

  def genDecimalInput[A: Numeric, U](g: Gen[A], u: U): Gen[DecimalInput[U]] =
    g.map(a => DecimalInput[U](Numeric[A].toDouble(a), u))

  implicit def cogLongInput[U: Cogen]: Cogen[LongInput[U]] =
    Cogen[(Long, U)].contramap { li =>
      (li.value, li.units)
    }

  implicit def cogDecimalInput[U: Cogen]: Cogen[DecimalInput[U]] =
    Cogen[(BigDecimal, U)].contramap { di =>
      (di.value, di.units)
    }

}

object GenNumericUnitsInput extends GenNumericUnitsInput
