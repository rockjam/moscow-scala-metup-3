Typeclass: center, middle, inverse
# Typeclasses in scala

---
class: center, middle, inverse
# Take 1


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
### • explicit
### • local
### • from imports
### • inhertited
### • from package object

---

style: middle
## Проблемы

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
## Убираем костыли

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
## Парсинг  структур данных

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
implicit object HNilParser extends Parser[HNil] {
  def parse = { s => if (s.isEmpty) Some(HNil) else None }
}
```

--

```scala
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
```

---

```scala
implicit def caseClassParser[A, R <: HList](implicit
                                            gen: Generic[A] { type Repr = R },
                                            reprParser: Parser[R]): Parser[A] = 
new Parser[A] {
  def parse: String => Option[A] = { s => reprParser.parse(s) map gen.from }
}
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