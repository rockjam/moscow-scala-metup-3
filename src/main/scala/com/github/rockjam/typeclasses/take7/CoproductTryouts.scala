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

package com.github.rockjam.typeclasses.take7

object CoproductTryouts {
  import shapeless._
  import scala.annotation.implicitNotFound
  import scala.language.implicitConversions

  @implicitNotFound(Console.RED + "No instance of typeclass Parser found for type" + Console.BLUE + " ${A}" + Console.RESET)
  trait Encoder[A] {
    def encode(a: A): Json
  }

  object Encoder extends ProductTypeClassCompanion[Encoder] {
    // typeclasses for primitive types
    implicit final val doubleEncoder: Encoder[Double] = JsNumber.apply
    implicit final val floatEncoder: Encoder[Float] = { f => JsNumber(f.toDouble) }
    implicit final val longEncoder: Encoder[Long] = { l => JsNumber(l.toDouble) }
    implicit final val intEncoder: Encoder[Int] = { i => JsNumber(i.toDouble) }
    implicit final val shortEncoder: Encoder[Short] = { s => JsNumber(s.toDouble) }
    implicit final val byteEncoder: Encoder[Byte] = { b => JsNumber(b.toDouble) }
    implicit final val charEncoder: Encoder[Char] = { c => JsString(c.toString) }
    implicit final val unitEncoder: Encoder[Unit] = { _ => JsNull }
    implicit final val booleanEncoder: Encoder[Boolean] = JsBoolean.apply
    implicit final val stringEncoder: Encoder[String] = JsString.apply

    implicit final val noneEncoder: Encoder[None.type] = { _ => JsNull }
    implicit def someEncoder[A: Encoder]: Encoder[Some[A]] = { some =>
      Encoder[A].encode(some.value)
    }

    object typeClass extends ProductTypeClass[Encoder] {
      def product[H, T <: HList](ch: Encoder[H], ct: Encoder[T]): Encoder[H :: T] = { hList =>
        val JsObject(pairs) = ct.encode(hList.tail) // not too safe
        JsObject(pairs + (scala.util.Random.nextString(4) -> ch.encode(hList.head)))
      }
      def emptyProduct: Encoder[HNil] = { _ => JsObject.empty }
      def project[F, G](instance: => Encoder[G], to: (F) => G, from: (G) => F): Encoder[F] = { f =>
        instance.encode(to(f))
      }
    }

    implicit val cnilEncoder: Encoder[CNil] = { _ => JsObject.empty }

    implicit def cConsEncoder[L, R <: Coproduct](implicit
      lEncoder: Encoder[L],
      rEncoder: Encoder[R]): Encoder[L :+: R] = {
      case Inl(l) => lEncoder.encode(l)
      case Inr(r) => rEncoder.encode(r)
    }

  }

  final class EncoderOps[A](val toEncode: A) extends AnyVal {
    def asJson(implicit encoder: Encoder[A]): Json = encoder.encode(toEncode)
  }

  object EncoderSyntax {
    implicit def encoderOps[A: Encoder](a: A) = new EncoderOps[A](a)
  }
  import EncoderSyntax._

  final case class Person(name: String, lastName: String, age: Int)

  sealed trait Item extends Product with Serializable
  final case class NamedItem(id: Int, name: String, model: Option[String]) extends Item
  final case class ExpiringItem(id: Int, expirationDate: String) extends Item
  final case class PlainItem(id: Int) extends Item
  final case class PricyItem(id: Int, price: Long) extends Item

  val intJson = 2.asJson
  val stringJson = "hello".asJson
  val personJson = Person("nick", "cave", 50).asJson

  val item: Item = NamedItem(22, "Pen", Some("Bic"))

  val itemJson = item.asJson
  val namedItemJson = NamedItem(33, "Pen", None).asJson

}
