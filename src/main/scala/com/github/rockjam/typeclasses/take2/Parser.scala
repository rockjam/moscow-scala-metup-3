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

package com.github.rockjam.typeclasses.take2

import scala.util.Try

/**
 * вместо использования implicit параметров
 * используем context bounds и implicitly для
 * "призыва" инстанса тайпклассов
 */

// typeclass
trait Parser[T] {
  def parse: String => Option[T]
}

object Parser {

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

  implicit def pairParser[A: Parser, B: Parser] = new Parser[(A, B)] {
    def parse = { s =>
      val arr = s split ","
      for {
        a <- arr.lift(0)
        b <- arr.lift(1)
        ap <- implicitly[Parser[A]].parse(a)
        bp <- implicitly[Parser[B]].parse(b)
      } yield ap -> bp
    }
  }

}
