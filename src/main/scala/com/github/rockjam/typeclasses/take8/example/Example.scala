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
package example

object Example {
  import JsonEncoderSyntax._

  final case class Person(name: String, lastName: String, age: Int)
  final case class Song(title: String, artist: String, duration: Int)

  sealed trait Item extends Product with Serializable
  final case class NamedItem(id: Int, name: String, model: Option[String]) extends Item
  final case class ExpiringItem(id: Int, expirationDate: String) extends Item
  final case class PlainItem(id: Int) extends Item
  final case class PricyItem(id: Int, price: Long) extends Item
  final case class NestedItem(id: Int, other: Item) extends Item

  val intJson = 2.asJson
  val stringJson = "hello".asJson
  val personJson = Person("nick", "cave", 50).asJson
  val songJson = Song("man who sold the world", "David Bowie", 209).asJson

  val item: Item = NamedItem(22, "Pen", Some("Bic"))

  val itemJson = item.asJson

  val namedItemJson = NamedItem(33, "Pen", None).asJson

  val nested: Item = NestedItem(33, PricyItem(44, 20000))
  val nestedJson = nested.asJson

}
