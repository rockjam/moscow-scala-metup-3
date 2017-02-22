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

object MonoidTryouts {

  import scala.language.implicitConversions
  import shapeless._

  trait Monoid[T] {
    def zero: T
    def append(a: T, b: T): T
  }

  object Monoid extends ProductTypeClassCompanion[Monoid] {
    def mzero[T](implicit mt: Monoid[T]): T = mt.zero

    implicit val booleanMonoid: Monoid[Boolean] = new Monoid[Boolean] {
      override def zero: Boolean = false
      override def append(a: Boolean, b: Boolean): Boolean = a || b
    }
    implicit val intMonoid: Monoid[Int] = new Monoid[Int] {
      override def zero: Int = 0
      override def append(a: Int, b: Int): Int = a + b
    }
    implicit val doubleMonoid: Monoid[Double] = new Monoid[Double] {
      override def zero: Double = 0.0
      override def append(a: Double, b: Double): Double = a + b
    }

    implicit val stringMonoid: Monoid[String] = new Monoid[String] {
      override def zero: String = ""
      override def append(a: String, b: String): String = a + b
    }

    object typeClass extends ProductTypeClass[Monoid] {
      override def product[H, T <: HList](ch: Monoid[H], ct: Monoid[T]): Monoid[H :: T] = new Monoid[H :: T] {
        override def zero: H :: T = ch.zero :: ct.zero
        override def append(a: H :: T, b: H :: T): H :: T = ch.append(a.head, b.head) :: ct.append(a.tail, b.tail)
      }

      override def emptyProduct: Monoid[HNil] = new Monoid[HNil] {
        override def zero: HNil = HNil
        override def append(a: HNil, b: HNil): HNil = HNil
      }

      override def project[F, G](instance: => Monoid[G], to: (F) => G, from: (G) => F): Monoid[F] = new Monoid[F] {
        override def zero: F = from(instance.zero)
        override def append(a: F, b: F): F = from(instance.append(to(a), to(b)))
      }
    }
  }

  trait MonoidSyntax[T] {
    def |+|(b: T): T
  }

  object MonoidSyntax {
    implicit def monoidSyntax[T: Monoid](a: T): MonoidSyntax[T] = { b: T => implicitly[Monoid[T]].append(a, b) }
  }
  import MonoidSyntax._

  case class Person(name: String, lastName: String, age: Int)

  val nick = Person("nick", "cave", 24)
  val other = Person("foo", "bar", 44)

  Monoid[Person].append(nick, other) == (nick |+| other)

}
