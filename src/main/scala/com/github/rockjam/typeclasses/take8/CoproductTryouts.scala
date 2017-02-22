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

object CoproductTryouts {
  import shapeless._

  import scala.annotation.implicitNotFound
  import scala.language.implicitConversions

  @implicitNotFound(Console.RED + "No instance of typeclass Parser found for type" + Console.BLUE + " ${A}" + Console.RESET)
  trait JsonEncoder[A] {
    def encode(a: A): Json
  }

  object JsonEncoder extends LabelledTypeClassCompanion[JsonEncoder] {
    // typeclasses for primitive types
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

    implicit final val noneEncoder: JsonEncoder[None.type] = { _ => JsNull }
    implicit def someEncoder[A: JsonEncoder]: JsonEncoder[Some[A]] = { some =>
      JsonEncoder[A].encode(some.value)
    }

    object typeClass extends LabelledTypeClass[JsonEncoder] {
      def coproduct[L, R <: Coproduct](name: String, cl: => JsonEncoder[L], cr: => JsonEncoder[R]): JsonEncoder[L :+: R] = { co =>
        co match {
          case Inl(l) => cl.encode(l)
          case Inr(r) => cr.encode(r)
        }
      }

      def emptyCoproduct: JsonEncoder[CNil] = { _ => JsObject.empty }

      def product[H, T <: HList](name: String, ch: JsonEncoder[H], ct: JsonEncoder[T]): JsonEncoder[H :: T] = { hList =>
        val JsObject(pairs) = ct.encode(hList.tail) // not too safe
        val headPair = name -> ch.encode(hList.head)
        JsObject(pairs + headPair)
      }
      def emptyProduct: JsonEncoder[HNil] = { _ => JsObject.empty }
      def project[F, G](instance: => JsonEncoder[G], to: (F) => G, from: (G) => F): JsonEncoder[F] = { f =>
        instance.encode(to(f))
      }
    }
  }

  final class EncoderOps[A](val toEncode: A) extends AnyVal {
    def asJson(implicit encoder: JsonEncoder[A]): Json = encoder.encode(toEncode)
  }

  object EncoderSyntax {
    implicit def encoderOps[A: JsonEncoder](a: A) = new EncoderOps[A](a)
  }
  import EncoderSyntax._

  final case class Person(name: String, lastName: String, age: Int)

  sealed trait Item extends Product with Serializable
  final case class NamedItem(id: Int, name: String, model: Option[String]) extends Item
  final case class ExpiringItem(id: Int, expirationDate: String) extends Item
  final case class PlainItem(id: Int) extends Item
  final case class PricyItem(id: Int, price: Long) extends Item
  final case class NestedItem(id: Int, other: Item) extends Item

  val intJson = 2.asJson
  val stringJson = "hello".asJson
  val personJson = Person("nick", "cave", 50).asJson

  val item: Item = NamedItem(22, "Pen", Some("Bic"))

  val itemJson = item.asJson
  val namedItemJson = NamedItem(33, "Pen", None).asJson

  val nested: Item = NestedItem(33, PricyItem(44, 20000))
  val nestedJson = nested.asJson

}
