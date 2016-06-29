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

package com.github.rockjam.typeclasses.take4.example

import com.github.rockjam.typeclasses.take4.Parser

object Example {
  def parseSeq[A: Parser](input: Seq[String]): Seq[A] = {
    input flatMap { s => Parser[A].parse(s) }
  }

  def parseNonEmptySeq[A: Parser](input: Seq[String]): Seq[A] = {
    if (input.isEmpty) Seq.empty[A]
    else parseSeq(input)
  }

  parseSeq[Int](List("1", "2", "3", "4"))

  //  parseSeq[BigDecimal](List("1.00002", "2.00010002", "3.00002", "4.0005"))

  def parsePair[A: Parser, B: Parser](s: String): Option[(A, B)] =
    Parser[(A, B)].parse(s)

  parsePair[Int, String]("2,scala")

  val twentyTwo = Parser[Int].parse("22")
  val hello = Parser[String].parse("Hello")
  val twentyTwoString = Parser[String].parse("22")

  //предмет,цена,количество
  val reportList = List(
    "ручки,10.00,200",
    "степлеры,100.50,34",
    "арбузы,300.00,5"
  )

  def buildReport(data: List[String]): String = {
    val parsedItems: List[(String, Double, Int)] = data flatMap { str =>
      val parts = str.split(",")
      for {
        name <- parts.lift(0)
        strPrice <- parts.lift(1)
        strQuaunt <- parts.lift(2)
        price <- Parser[Double].parse(strPrice)
        quant <- Parser[Int].parse(strQuaunt)
      } yield (name, price, quant)
    }

    val total: Double = parsedItems map (item => item._2 * item._3) sum

    val subtotals = (parsedItems map {
      case (name, price, quant) =>
        s"${name}: ${price * quant}"
    }) :+ s"Total is: $total"

    subtotals mkString "\n"
  }

  println(buildReport(reportList))

}
