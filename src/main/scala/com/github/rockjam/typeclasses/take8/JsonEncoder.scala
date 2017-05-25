/*
 * Copyright 2016 Nikolay Tatarinov
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

package com.github.rockjam.typeclasses.take8

import shapeless._
import shapeless.labelled.FieldType

import scala.annotation.implicitNotFound
import scala.language.implicitConversions

@implicitNotFound(Console.RED + "No instance of typeclass JsonEncoder found for type" + Console.BLUE + " ${A}" + Console.RESET)
trait JsonEncoder[A] {
  def encode(a: A): Json
}

object JsonEncoder extends LabelledTypeClassCompanion[JsonEncoder] {

  // typeclass instances for primitive types
  implicit final val doubleEncoder: JsonEncoder[Double] = JsNumber.apply
  implicit final val floatEncoder: JsonEncoder[Float] = { f => JsNumber(f.toDouble) }
  implicit final val longEncoder: JsonEncoder[Long] = { l => JsNumber(l.toDouble) }
  implicit final val intEncoder: JsonEncoder[Int] = { i => JsNumber(i.toDouble) }
  implicit final val shortEncoder: JsonEncoder[Short] = { s => JsNumber(s.toDouble) }
  implicit final val byteEncoder: JsonEncoder[Byte] = { b => JsNumber(b.toDouble) }
  implicit final val charEncoder: JsonEncoder[Char] = { c => JsString(c.toString) }
  implicit final val unitEncoder: JsonEncoder[Unit] = { _ => JsNull }
  implicit final val booleanEncoder: JsonEncoder[Boolean] = JsBoolean.apply
  implicit final val stringEncoder: JsonEncoder[String] = JsString.apply

  // typeclass instances for Option[A]
  implicit final val noneEncoder: JsonEncoder[None.type] = { _ => JsNull }

  implicit final def someEncoder[A: JsonEncoder]: JsonEncoder[Some[A]] = { some =>
    JsonEncoder[A].encode(some.value)
  }

  object typeClass extends LabelledTypeClass[JsonEncoder] {
    def coproduct[L, R <: Coproduct](
      name: String,
      cl: => JsonEncoder[L],
      cr: => JsonEncoder[R]
    ): JsonEncoder[L :+: R] = { co =>
      co match {
        case Inl(l) => cl.encode(l)
        case Inr(r) => cr.encode(r)
      }
    }

    def emptyCoproduct: JsonEncoder[CNil] = { _ => JsObject.empty }

    def product[H, T <: HList](
      name: String,
      ch: JsonEncoder[H],
      ct: JsonEncoder[T]
    ): JsonEncoder[H :: T] = { hList =>
      val JsObject(pairs) = ct.encode(hList.tail) // not too safe
      val headPair = name -> ch.encode(hList.head)
      JsObject(pairs + headPair)
    }
    def emptyProduct: JsonEncoder[HNil] = { _ => JsObject.empty }
    def project[F, G](
      instance: => JsonEncoder[G],
      to: F => G,
      from: G => F
    ): JsonEncoder[F] = { f =>
      instance.encode(to(f))
    }
  }

}

