class: center, middle, inverse
# Typeclasses in scala

---
class: center, middle, inverse
# Take 1

---
# Пример

Нужно написать парсер из строки в определенные нами структуры данных.

Например:
```scala
val toParse = "hello,world,42"
case class Message(first: String, second: String, id: Int)
val result: Option[Message] = Parser.parse[Message](toParse)
```

---
layout: true
# Typeclass encoding take 1
---

```scala
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
}
```

---

```scala
implicit def pairParser[A, B](implicit aParser: Parser[A], bParser: Parser[B]) = 
new Parser[(A, B)] {
  def parse = { s =>
    val arr = s split ","
    for {
      a <- arr.lift(0)
      b <- arr.lift(1)
      ap <- aParser.parse(a)
      bp <- bParser.parse(b)
    } yield ap -> bp
  }
}
```

---

```scala
  def parseSeq[A](input: Seq[String])(
    implicit aParser: Parser[A]): Seq[A] = {
    input flatMap { s => aParser.parse(s) }
  }

  def parseNonEmptySeq[A](input: Seq[String])(
    implicit aParser: Parser[A]): Seq[A] = {
    if (input.isEmpty) Seq.empty[A]
    else parseSeq(input)
  }
  
  def parsePair[A, B](s: String)(
    implicit pairParser: Parser[(A, B)]): Option[(A, B)] =
    pairParser.parse(s)

  parseSeq[Int](List("1", "2", "3", "4"))
  parseSeq[BigDecimal](List("1.00002", "2.00010002", 
    "3.00002", "4.0005")) // won't compile
```

---

```scala
scala>   parseSeq[BigDecimal](List("1.00002", "2.00010002", "3.00002", "4.0005"))
<console>:16: error: could not find implicit value for parameter aParser: 
	com.github.rockjam.typeclasses.take1.Parser[BigDecimal]
         parseSeq[BigDecimal](List("1.00002", "2.00010002", "3.00002", "4.0005"))
```

---

##Implicit resolution:

--

### • explicit

--

### • local

--

### • from imports

--

### • inhertited

--

### • from package object

---

style: middle
## Проблемы:

--

### • implicit parser повсюду

--

### • и распространяется!

--

### • сообщения об ошибках

--

### • implicit не только для typeclasses

---
layout: false
class: center, middle, inverse
# Take 2
## Можно лучше

---
layout: true
# Typeclass encoding take 2

---
## Context bounds (since scala 2.8.0)

```scala
  implicit def pairParser[A: Parser, B: Parser] = new Parser[(A, B)] {
    def parse = { s =>
      val arr = s split ","
      for {
        a <- arr.lift(0)
        b <- arr.lift(1)
*       ap <- implicitly[Parser[A]].parse(a) // NOT GOOD!
*       bp <- implicitly[Parser[B]].parse(b) // NOT GOOD!
      } yield ap -> bp
    }
  }
```

---
## Context bounds

```scala
def parseSeq[A: Parser](input: Seq[String]): Seq[A] = {
*  input flatMap { s => implicitly[Parser[A]].parse(s) }
}
def parseNonEmptySeq[A: Parser](input: Seq[String]): Seq[A] = {
  if (input.isEmpty) Seq.empty[A]
  else parseSeq(input)
}
def parsePair[A: Parser, B: Parser](s: String): Option[(A, B)] =
*  implicitly[Parser[(A, B)]].parse(s)
```

---
## Context bounds are syntactic sugar

```scala
pairParser: [A, B](
  implicit evidence$1: com.github.rockjam.typeclasses.take2.Parser[A],
  implicit evidence$2: com.github.rockjam.typeclasses.take2.Parser[B]
)com.github.rockjam.typeclasses.take2.Parser[(A, B)]
```

---
## Избавляемся от implicitly

```scala
object Parser {

  def apply[T:Parser]: Parser[T] = implicitly[Parser[T]]
  ...
  ...
}
```

---
## Избавляемся от implicitly

```scala
  implicit def pairParser[A: Parser, B: Parser] = new Parser[(A, B)] {
    def parse = { s =>
      val arr = s split ","
      for {
        a <- arr.lift(0)
        b <- arr.lift(1)
*       ap <- Parser[A].parse(a) // GOOD
*       bp <- Parser[B].parse(b) // GOOD
      } yield ap -> bp
    }
  }
```

---
## Избавляемся от implicitly

```scala
def parseSeq[A: Parser](input: Seq[String]): Seq[A] = {
* input flatMap { s => Parser[A].parse(s) }
}

def parseNonEmptySeq[A: Parser](input: Seq[String]): Seq[A] = {
  if (input.isEmpty) Seq.empty[A]
  else parseSeq(input)
}

def parsePair[A: Parser, B: Parser](s: String): Option[(A, B)] =
* Parser[(A, B)].parse(s)
```

---
## Улучшаем сообщение об ошибке

```scala
// typeclass
@implicitNotFound("No member of typeclass Parser found for type ${T}")
trait Parser[T] {
  def parse: String => Option[T]
}
```

```scala
scala> parseSeq[BigDecimal](List("1.00002", "2.00010002", "3.00002", "4.0005"))
<console>:16: error: No instance of typeclass Parser found for type BigDecimal
       parseSeq[BigDecimal](List("1.00002", "2.00010002", "3.00002", "4.0005"))
                           ^
```

---
## User-defined typeclasses

```scala
implicit object bigDecimalParser extends Parser[BigDecimal] {
  def parse = { s => Try(BigDecimal(s)).toOption }
}
scala> parseSeq[BigDecimal](List("1.00002", "2.00010002", "3.00002", "4.0005"))
res5: Seq[BigDecimal] = List(1.00002, 2.00010002, 3.00002, 4.0005)
```

---
layout: false
class: center, middle, inverse
# Take 3
## Еще немного лучше

---
layout: true
# Typeclass encoding take 3

---
## Boilerplate 👇

```scala
  implicit object ByteParser extends Parser[Byte] {
*   def parse = { s => Try(s.toByte).toOption }
  }
  implicit object ShortParser extends Parser[Short] {
*   def parse = { s => Try(s.toShort).toOption }
  }
  implicit object IntParser extends Parser[Int] {
*   def parse = { s => Try(s.toInt).toOption }
  }
  implicit object LongParser extends Parser[Long] {
*   def parse = { s => Try(s.toLong).toOption }
  }
  implicit object FloatParser extends Parser[Float] {
*   def parse = { s => Try(s.toFloat).toOption }
  }
  implicit object DoubleParser extends Parser[Double] {
*   def parse = { s => Try(s.toDouble).toOption }
  }
  implicit object BoolParser extends Parser[Boolean] {
*   def parse = { s => Try(s.toBoolean).toOption }
  }
  ....
```

---
## Helper function

```scala
object Parser {
  final def instance[T](f: String => Option[T]): Parser[T] = new Parser[T] {
    def parse = { from: String => f(from) }
  }
  ...
}
```

--


```scala
implicit val ByteParser   = Parser.instance[Byte] { s => Try(s.toByte).toOption }
implicit val ShortParser  = Parser.instance[Short] { s => Try(s.toShort).toOption }
implicit val IntParser    = Parser.instance[Int] { s => Try(s.toInt).toOption }
implicit val LongParser   = Parser.instance[Long] { s => Try(s.toLong).toOption }
implicit val FloatParser  = Parser.instance[Float] { s => Try(s.toFloat).toOption }
implicit val DoubleParser = Parser.instance[Double] { s => Try(s.toDouble).toOption }
implicit val BoolParser   = Parser.instance[Boolean] { s => Try(s.toBoolean).toOption }
implicit val CharParser   = Parser.instance[Char] {
	case s if s.length == 1 => Some(s.charAt(0))
	case _                  => None
}
implicit val StringParser = Parser.instance[String] { Some.apply }
```

---
##  SAM (since 2.12.0)

```scala
trait Parser[T] {
  def parse(from: String): Option[T]
}
```

--

```scala
val myParser: Parser[BigDecimal] = { s: String  => Try(BigDecimal(s)).toOption }
```

--

```scala
  implicit val byteParser:    Parser[Byte] = { s => Try(s.toByte).toOption }
  implicit val shortParser:   Parser[Short] = { s => Try(s.toShort).toOption }
  implicit val intParser:     Parser[Int] = { s => Try(s.toInt).toOption }
  implicit val longParser:    Parser[Long] = { s => Try(s.toLong).toOption }
  implicit val floatParser:   Parser[Float] = { s => Try(s.toFloat).toOption }
  implicit val doubleParser:  Parser[Double] = { s => Try(s.toDouble).toOption }
  implicit val booleanParser: Parser[Boolean] = { s => Try(s.toBoolean).toOption }
  implicit val stringParser:  Parser[String] = Some.apply[String]
  implicit val charParser:    Parser[Char] = { s =>
     if (s.length == 1) Some(s.charAt(0)) else None
  }
```

---
layout: false
class: center, middle, inverse
# Take 4
## Парсинг  структур данных
---
class: middle

### Отчет:
```scala
Наименование | Цена   | Количество
-------------|--------|-----------
ручки        | 10.00  |    200     
степлеры     | 100.50 |    34
арбузы       | 300.00 |    50
```

--

### Результат:
```scala
ручки: 2000.0
степлеры: 3417.0
арбузы: 1500.0
Total is: 6917.0
```


---
layout: true
# Парсинг  структур данных

---

```scala
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
```

---

```scala
scala>   val reportList = List(
     |     "ручки,10.00,200",
     |     "степлеры,100.50,34",
     |     "арбузы,300.00,5"
     |   )
reportList: List[String] = List(ручки,10.00,200, степлеры,100.50,34, арбузы,300.00,5)

scala> println(buildReport(reportList))
ручки: 2000.0
степлеры: 3417.0
арбузы: 1500.0
Total is: 6917.0  
```

---

```scala
case class Item(name: String, price: Double, quantity: Int)
```

--

```scala
def buildReport(data: List[String]): String = {
  val parsedItems: List[Item] = data flatMap { str =>
    Parser[Item].parse(str)
  }

  val total: Double = parsedItems map (item => item.price * item.quantity) sum

  val subtotals = (parsedItems map {
    case Item(name, price, quant) =>
      s"$name: ${price * quant}"
  }) :+ s"Total is: $total"

  subtotals mkString "\n"
}
```
--

```scala
error: No instance of typeclass Parser found for type 
	com.github.rockjam.typeclasses.take5.example.Example.Item
```

---
## Typeclass generic derivation

```scala
scala> import shapeless._
import shapeless._

scala> case class Item(name: String, price: Double, quantity: Int)
defined class Item

scala> Generic[Item]
res1: shapeless.Generic[Item]{type Repr = String :: Double :: Int :: HNil}
```

---

```scala
implicit val hNilParser: Parser[HNil] = { s => if (s.isEmpty) Some(HNil) else None }
```

--

```scala
implicit def hconsParser[H: Parser, T <: HList: Parser] = new Parser[H :: T] {
  def parse(from: String): Option[H :: T] = {
    from.split(",").toList match {
      case head +: tail =>
        for {
          h <- Parser[H].parse(head)
          t <- Parser[T].parse(tail mkString ",")
        } yield h :: t
    }
  }
}
```

---

```scala
implicit def caseClassParser[A, R <: HList](
	implicit
	gen: Generic[A] { type Repr = R },
	reprParser: Parser[R]): Parser[A] = { s: String => 
		reprParser.parse(s) map gen.from 
	}
```

--


```scala
scala> case class Foo(a: Int, b: String)
defined class Foo
scala> val fooGen = Generic[Foo]
fooGen: shapeless.Generic[Foo]{type Repr = Int :: String ::HNil} = anon$macro$9$1@288d618a
scala> val foo = fooGen.from(1 :: "Hello" :: HNil)
foo: Foo = Foo(1,Hello)
```

---

```scala
def buildReport(data: List[String]): String = {
  val parsedItems: List[Item] = data flatMap { str =>
    Parser[Item].parse(str)
  }

  val total: Double = parsedItems map (item => item.price * item.quantity) sum

  val subtotals = (parsedItems map {
    case Item(name, price, quant) =>
      s"$name: ${price * quant}"
  }) :+ s"Total is: $total"

  subtotals mkString "\n"
}

scala> println(buildReport(reportList))
ручки: 2000.0
степлеры: 3417.0
арбузы: 1500.0
Total is: 6917.0
```

---
layout: false
class: center, middle, inverse
# Example
## Json Encoding

---
## Json ADT

```scala
sealed trait Json
final case class  JsString(s: String)                extends Json
final case class  JsNumber(d: Double)                extends Json
final case class  JsBoolean(b: Boolean)              extends Json
final case object JsNull                             extends Json
final case class  JsObject(pairs: Map[String, Json]) extends Json
final case class  JsArray(values: List[Json])        extends Json
```

--

```scala
object JsObject {
  final val empty = JsObject(Map.empty[String, Json])
  def apply(pairs: (String, Json)*): JsObject = JsObject(pairs.toMap)
}
```

---
## Json Writer

```scala
object JsonWriter {

  final def write: Json => String = {
    case JsString(s)     => "\"" + s + "\""
    case JsNumber(n)     => n.toString
    case JsBoolean(b)    => b.toString
    case JsNull          => "null"
    case JsArray(values) => 
	  (values map write) mkString (start = "[", sep = ", ", end = "]")
    case JsObject(pairs) =>
      (pairs map {
        case (key, value) =>
          "\"" + key + "\"" + ": " + write(value)
      }) mkString (start = "{", sep = ", ", end = "}")
  }

}
```

---
## Json Encoder typeclass

```scala
@implicitNotFound("No instance of typeclass JsonEncoder found for type ${A}")
trait JsonEncoder[A] {
  def encode(a: A): Json
}
```

--

```scala
object JsonEncoder {
  def apply[T: JsonEncoder]: JsonEncoder[T] = implicitly[JsonEncoder[T]]

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

  implicit final def someEncoder[A: JsonEncoder]: JsonEncoder[Some[A]] = { some =>
    JsonEncoder[A].encode(some.value)
  }
}
```

---
## Json Encoder syntax

```scala
final class JsonEncoderOps[A](val toEncode: A) extends AnyVal {
  def asJson(implicit encoder: JsonEncoder[A]): Json = encoder.encode(toEncode)
}

object JsonEncoderSyntax {
  implicit def encoderOps[A: JsonEncoder](a: A) = new JsonEncoderOps[A](a)
}
```

--

```scala
scala> JsonEncoder[String].encode("foobar")
res1: com.github.rockjam.typeclasses.take8.Json = JsString(foobar)

scala> import JsonEncoderSyntax._
import JsonEncoderSyntax._

scala> "foobar".asJson
res2: com.github.rockjam.typeclasses.take8.Json = JsString(foobar)
```

---
## Json Encoder for Product

```scala
implicit def hNilEncoder: JsonEncoder[HNil] = { _ => JsObject.empty }

implicit def hconsEncoder[H, T <: HList](
  implicit
  hEncoder: JsonEncoder[H],
  tEncoder: JsonEncoder[T]
): JsonEncoder[H :: T] = { hList =>
  val JsObject(pairs) = tEncoder.value.encode(hList.tail) // not too safe
*  val headPair = hList.head.toString -> hEncoder.value.encode(hList.head)
  JsObject(pairs + headPair)
}

implicit def genericEncoder[A, R](
  implicit
  gen: Generic.Aux[A, R],
  encoder: JsonEncoder[R]): JsonEncoder[A] = { a => 
	  encoder.encode(gen.to(a)) 
  }
```

---
## Json Encoder for Product

```scala
final case class Person(name: String, lastName: String, age: Int)
final case class Song(title: String, artist: String, duration: Int)
```

--

```scala
scala> val personJson = Person("nick", "cave", 50).asJson
personJson: Json = 
  JsObject(Map(50 -> JsNumber(50.0), cave -> JsString(cave), nick -> JsString(nick)))
scala> val personWrited = JsonWriter.write(personJson)
personWrited: String = {"50": 50.0, "cave": "cave", "nick": "nick"}

scala> val songWrited = 
  JsonWriter.write(Song("man who sold the world", "David Bowie", 209).asJson)
songWrited: String = 
{
  "209": 209.0,
  "David Bowie": "David Bowie",
  "man who sold the world": "man who sold the world"
}
```

---
## Json Encoder for Product

```scala
final case class Person(name: String, lastName: String, age: Int)
final case class SongByPerson(title: String, artist: Person, duration: Int)
```

--

```scala
scala> val song = JsonWriter.write(
  SongByPerson(
  	"Man who sold the world", 
	Person("David", "Bowie", 50), 
	209
  ).asJson
)
song: String =
{
  "209": 209.0,
  "Person(David,Bowie,50)": {
    "50": 50.0,
    "Bowie": "Bowie",
    "David": "David"
  },
  "Man who sold the world": "Man who sold the world"
}
```

---
## Json Encoder 

```scala
sealed trait Item extends Product with Serializable
final case class NamedItem(id: Int, name: String, model: Option[String]) extends Item
final case class ExpiringItem(id: Int, expirationDate: String) extends Item
final case class PlainItem(id: Int) extends Item
final case class PricyItem(id: Int, price: Long) extends Item
final case class NestedItem(id: Int, other: Item) extends Item
```

--

```scala
scala> val item: Item = NamedItem(22, "Pen", Some("Bic"))
item: Item = NamedItem(22,Pen,Some(Bic))
scala> val itemJson = item.asJson
* <console>:21: error: value asJson is not a member of Item
       val itemJson = item.asJson
scala> JsonEncoder[Item].encode(itemJson)
* <console>:22: error: No instance of typeclass JsonEncoder found for type Item
       JsonEncoder[Item].encode(itemJson)
                  ^
```
---
## Product, Coproduct

### Product: `(bar: Int) & (baz: String)`
```scala
final case class Foo(bar: Int, baz: String) 
```

--
### Coproduct: `FooBar | FooBaz | FooQuix`

```scala
sealed trait Foo
final case class FooBar(i: Int, j: Int) extends Foo
final case class FooBaz(s: String, z: Boolean) extends Foo
final case class FooQuix(xs: List[Foo], x: Int) extends Foo
```
---
## Generic for Coproduct

```scala
sealed trait Item extends Product with Serializable
final case class NamedItem(id: Int, name: String, model: Option[String]) extends Item
final case class ExpiringItem(id: Int, expirationDate: String) extends Item
final case class PlainItem(id: Int) extends Item
final case class PricyItem(id: Int, price: Long) extends Item
final case class NestedItem(id: Int, other: Item) extends Item
```

--

```scala
scala> val gen = Generic[Item]
gen: shapeless.Generic[Item]{ type Repr = 
	ExpiringItem :+: 
	NamedItem :+: 
	NestedItem :+: 
	PlainItem :+: 
	PricyItem :+: 
	CNil
}
```

---
## Json Encoder for Coproduct

```scala
implicit def cNilEncoder: JsonEncoder[CNil] = { _ => JsObject.empty }

implicit def cConsEncoder[L, R <: Coproduct](implicit
  lEncoder: Lazy[JsonEncoder[L]],
  rEncoder: Lazy[JsonEncoder[R]]): JsonEncoder[L :+: R] = { co =>
  co match {
    case Inl(l) => lEncoder.value.encode(l)
    case Inr(r) => rEncoder.value.encode(r)
  }
}
```

--

```scala
scala> val item: Item = NamedItem(22, "Pen", Some("Bic"))
item: Item = NamedItem(22,Pen,Some(Bic))
scala> val itemJson = item.asJson
*itemJson: Json = 
*  JsObject(Map(Some(Bic) -> JsString(Bic), Pen -> JsString(Pen), 22 -> JsNumber(22.0)))
scala> val itemWrited = JsonWriter.write(itemJson)
*itemWrited: String = {"Some(Bic)": "Bic", "Pen": "Pen", "22": 22.0}
scala> val namedItemWrited = JsonWriter.write(NamedItem(33, "Pen", None).asJson)
*namedItemWrited: String = {"None": null, "Pen": "Pen", "33": 33.0}
```

---
### Labelled Generic for names

```scala
final case class Person(name: String, lastName: String, age: Int)
```

--

```scala
scala> import shapeless._
import shapeless._
scala> val gen = LabelledGeneric[Person]
gen: shapeless.LabelledGeneric[Person]{
	type Repr = {
	  'name ->  String ::
	  'lastName -> String ::
	  'age -> Int ::
	  HNil
	} 
```
---
### Labelled Generic for names

```scala
implicit def hNilEncoder: JsonEncoder[HNil] = { _ => JsObject.empty }

implicit def hconsEncoder[K <: Symbol, H, T <: HList](
  implicit
  witness: Witness.Aux[K],
  hEncoder: Lazy[JsonEncoder[H]],
  tEncoder: Lazy[JsonEncoder[T]]
): JsonEncoder[FieldType[K, H] :: T] = { hList =>
  val JsObject(pairs) = tEncoder.value.encode(hList.tail) // not too safe
  val headPair = witness.value.name -> hEncoder.value.encode(hList.head)
  JsObject(pairs + headPair)
}

implicit def deriveEncoder[A, R](
  implicit
  gen: LabelledGeneric.Aux[A, R],
  encoder: Lazy[JsonEncoder[R]]
): JsonEncoder[A] = { a => encoder.value.encode(gen.to(a)) }
```

---
### Labelled Generic for names

```scala
final case class Person(name: String, lastName: String, age: Int)
final case class SongByPerson(title: String, artist: Person, duration: Int)
```

--

```scala
scala> val songJson = 
	SongByPerson("Man who sold the world", Person("David", "Bowie", 50), 209).asJson
songJson: Json = JsObject(
	Map(
		duration -> JsNumber(209.0), 
		artist -> JsObject(Map(
			age -> JsNumber(50.0), 
			lastName -> JsString(Bowie), 
			name -> JsString(David))
		), 
		title -> JsString(Man who sold the world)
	)
)

scala> val songWrited = JsonWriter.write(songJson)
songWrited: String = 
{
  "duration": 209.0,
  "artist": {
    "age": 50.0,
    "lastName": "Bowie",
    "name": "David"
  },
  "title": "Man who sold the world"
}
```


---
layout: false
class: center, middle, inverse
# Все сделано за нас

---
## Shapeless тайп класс TypeClass

```scala
object JsonEncoder extends LabelledTypeClassCompanion[JsonEncoder] {
  // ...
  object typeClass extends LabelledTypeClass[JsonEncoder] {
    def coproduct[L, R <: Coproduct](
      name: String,
      cl: => JsonEncoder[L],
      cr: => JsonEncoder[R]
    ): JsonEncoder[L :+: R] = {
        case Inl(l) => cl.encode(l)
        case Inr(r) => cr.encode(r)
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
```

---
layout: false
class: center, middle, inverse
# Спасибо за внимание 🕺🏻

---
layout: false
class: center, middle, inverse
# Вопросы?
