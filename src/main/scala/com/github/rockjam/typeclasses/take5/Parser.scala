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

package com.github.rockjam.typeclasses.take5

import scala.annotation.implicitNotFound
import scala.util.Try
import shapeless._

/**
 * вместо использования implicit параметров
 * используем context bounds и implicitly для
 * "призыва" инстанса тайпклассов
 */

// typeclass
@implicitNotFound("No instance of typeclass Parser found for type ${T}")
trait Parser[T] {
  def parse: String => Option[T]
}

object Parser {

  def apply[T: Parser]: Parser[T] = implicitly[Parser[T]]

  // typeclass instances
  implicit object IntParser extends Parser[Int] {
    def parse = { s => Try(s.toInt).toOption }
  }

  implicit object DoubleParser extends Parser[Double] {
    def parse = { s => Try(s.toDouble).toOption }
  }

  implicit object CharParser extends Parser[Char] {
    def parse = {
      case s if s.length == 1 => Some(s.charAt(0))
      case _                  => None
    }
  }

  implicit object StringParser extends Parser[String] {
    def parse = Some.apply
  }

  implicit object HNilParser extends Parser[HNil] {
    def parse = { s => if (s.isEmpty) Some(HNil) else None }
  }

  implicit def hconsParser[H: Parser, T <: HList: Parser] = new Parser[H :: T] {
    def parse: String => Option[H :: T] = { s =>
      s.split(",").toList match {
        case head +: tail =>
          for {
            h <- Parser[H].parse(head)
            t <- Parser[T].parse(tail mkString ",")
          } yield h :: t
      }
    }
  }

  implicit def caseClassParser[A, R <: HList](implicit
    gen: Generic[A] { type Repr = R },
    reprParser: Parser[R]): Parser[A] = new Parser[A] {
    def parse: String => Option[A] = { s => reprParser.parse(s) map gen.from }
  }

}
