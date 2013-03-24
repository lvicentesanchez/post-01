package futuremonad

import scala.language.{ higherKinds, postfixOps }

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.immutable._

import scalaz._
import scalaz.std.list._
import scalaz.syntax.monad._
import scalaz.syntax.traverse._

object Monads extends App {

  implicit val futureMonad = new Monad[Future] {
    override def point[A](a: => A): Future[A] =
      future(a)

    override def bind[A, B](fa: Future[A])(f: A => Future[B]): Future[B] =
      fa.flatMap(f)
  }
  
  def sequence[T, M[+_] : Monad](l: List[M[T]]): M[List[T]] =
    implicitly[Monad[M]].sequence(l)

  val nums = (1 to 100) toStream
  val even = nums filter (_ % 2 == 0) toList
  val odds = nums filter (_ % 2 == 1) toList
  
  val fls1 = sequence(even map (a => future(a))) map (_.sum) 
  val fls2 = odds.map(a => future(a*2)).sequence map (_.sum) 
  
  fls1 onSuccess {
    case total => println(s"Sum of evens: $total")
  }

  fls2 onSuccess {
    case total => println(s"Sum of 2*odds: $total")
  }

  Await.ready(fls1, 5 seconds)
  Await.ready(fls2, 5 seconds)
}