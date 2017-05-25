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

sealed trait Json
final case class JsString(s: String) extends Json
final case class JsNumber(d: Double) extends Json
final case class JsBoolean(b: Boolean) extends Json
final case object JsNull extends Json
final case class JsObject(pairs: Map[String, Json]) extends Json
final case class JsArray(values: List[Json]) extends Json

object JsObject {
  final val empty = JsObject(Map.empty[String, Json])
  def apply(pairs: (String, Json)*): JsObject = JsObject(pairs.toMap)
}

object JsonWriter {

  final def write: Json => String = {
    case JsString(s)     => "\"" + s + "\""
    case JsNumber(n)     => n.toString
    case JsBoolean(b)    => b.toString
    case JsNull          => "null"
    case JsArray(values) => (values map write) mkString (start = "[", sep = ", ", end = "]")
    case JsObject(pairs) =>
      (pairs map {
        case (key, value) =>
          "\"" + key + "\"" + ": " + write(value)
      }) mkString (start = "{", sep = ", ", end = "}")
  }

}
