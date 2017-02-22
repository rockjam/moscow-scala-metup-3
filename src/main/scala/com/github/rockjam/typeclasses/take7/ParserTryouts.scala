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

import scala.util.Try

object ParserTryouts {

  import shapeless._

  trait Parser[T] {
    def parse(from: String): Option[T]
  }

  object Parser extends ProductTypeClassCompanion[Parser] {
    final def instance[T](f: String => Option[T]): Parser[T] = { from: String => f(from) }

    // default typeclass instances
    implicit val byteParser: Parser[Byte] = { s => Try(s.toByte).toOption }

    implicit val shortParser: Parser[Short] = { s => Try(s.toShort).toOption }

    implicit val intParser: Parser[Int] = { s => Try(s.toInt).toOption }

    implicit val longParser: Parser[Long] = { s => Try(s.toLong).toOption }

    implicit val floatParser: Parser[Float] = { s => Try(s.toFloat).toOption }

    implicit val doubleParser: Parser[Double] = { s => Try(s.toDouble).toOption }

    implicit val booleanParser: Parser[Boolean] = { s => Try(s.toBoolean).toOption }

    implicit val charParser: Parser[Char] = { s =>
      if (s.length == 1) Some(s.charAt(0)) else None
    }

    implicit val stringParser: Parser[String] = Some.apply[String]

    object typeClass extends ProductTypeClass[Parser] {
      override def product[H, T <: HList](ch: Parser[H], ct: Parser[T]): Parser[H :: T] =
        Parser.instance { from =>
          from.split(",").toList match {
            case h +: tail =>
              for {
                head <- ch.parse(h)
                tail <- ct.parse(tail.mkString(","))
              } yield head :: tail
          }
        }
      override def emptyProduct: Parser[HNil] = { s => if (s.isEmpty) Some(HNil) else None }
      override def project[F, G](instance: => Parser[G], to: (F) => G, from: (G) => F): Parser[F] =
        { s => instance.parse(s).map(from) }
    }
  }

  final case class Person(name: String, age: Int, averageScore: Double)

  Parser[Int].parse("3000")
  Parser[Long].parse("3000")
  Parser[Short].parse("3000")

  // знаем конкретно тип - Person
  Parser[Person].parse("nick,24,4.3")

}
