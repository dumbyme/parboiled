package org.parboiled

import org.parboiled.support.Characters
import _root_.scala.collection.mutable
import org.parboiled.{Action => PAction, Context => PContext}
import org.parboiled.matchers.{StringMatcher, CharSetMatcher}

trait Construction { this: Rules with Support =>

  trait Parser {
    private val cache = mutable.Map.empty[RuleMethod, Rule[Any]]

    def rule[T <: Rule[_]](block: => T): T = {
      val ruleMethod = getCurrentRuleMethod
      rule(ruleMethod.getMethodName, ruleMethod, block)
    }

    def rule[T <: Rule[_]](label: String)(block: => T): T = {
      rule(label, getCurrentRuleMethod, block)
    }

    private def rule[T <: Rule[_]](label: String, key: RuleMethod, block: => T): T = cache.get(key) match {
      case Some(rule) => rule.asInstanceOf[T]
      case None => {
        val proxy = new ProxyRule
        cache += key -> proxy // protect block from infinite recursion with a proxy
        val rule = block match { // evaluate block
          case r: NonLeafRule[_] => proxy.arm(r.withLabel(label).asInstanceOf[NonLeafRule[Nothing]])
          case r: LeafRule => r.withLabel(label)
          case _ => throw new IllegalStateException
        }
        cache += key -> rule
        rule.asInstanceOf[T]
      }
    }

  }

  def &(sub:Rule[_]) : NonLeafRule[Nothing] = new TestRule(sub)
  
  def !(sub:Rule[_]) : NonLeafRule[Nothing] = new TestNotRule(sub)

  def optional[V](sub:Rule[V]) : NonLeafRule[V] = new OptionalRule(sub)

  def zeroOrMore[V](sub:Rule[V]) : NonLeafRule[V] = new ZeroOrMoreRule(sub)

  def oneOrMore[V](sub:Rule[V]) : NonLeafRule[V] = new OneOrMoreRule(sub)

  def anyOf(s: String): LeafRule = anyOf(s.toCharArray)

  def anyOf(chars: Array[Char]): LeafRule = chars.length match {
    case 0 => EMPTY
    case 1 => toRule(chars(0))
    case _ => anyOf(Characters.of(chars: _*))
  }

  def anyOf(chars: Characters): LeafRule = {
    if (!chars.isSubtractive && chars.getChars().length == 1)
      toRule(chars.getChars()(0))
    else
      new LeafRule(new CharSetMatcher(chars).label(chars.toString))
  }

  case class Val[T](value: T)

  def withMatch(f: String => Unit) = f

  def withValue[V](f: V => Unit) = (value:Val[V]) => f(value.value)

  def testMatch(f: String => Boolean) = f

  def testValue[V](f: V => Boolean) = (value:Val[V]) => f(value.value)

  def set[V](f: => V) = () => Val(f)

  def setFromMatch[V](f: String => V) = (s:String) => Val(f(s))

  def setFromValue[V,T](f: V => T) = (value:Val[V]) => Val(f(value.value))

  def convertValue[V](f: V => V) = setFromValue(f)

  implicit def toRule(c: Char) = new CharRule(c)

  implicit def toRule(s: String): LeafRule = toRule(s.toCharArray)

  implicit def toRule[T](rule: Rule[T]) = rule.toMatcher

  implicit def toRule(chars: Array[Char]): LeafRule = chars.length match {
    case 0 => EMPTY
    case 1 => toRule(chars(0))
    case _ => new LeafRule(new StringMatcher(chars.map(toRule).map(_.toMatcher), chars).label("\"" + chars + '"'))
  }

  implicit def toAction[V, T](f: PContext[V] => Boolean): Rule[T] = {
    val action = new PAction[V] {
      def run(c: PContext[V]): Boolean = f(c.asInstanceOf[PContext[V]])
    }
    new ActionRule[T](action.asInstanceOf[PAction[T]])
  }

  implicit def toAction2[V, T](f: PContext[V] => Unit): Rule[T] =
    toAction((c: PContext[V]) => {f(c); true})

  implicit def toAction3[V, T](f: PContext[V] => Val[T]): Rule[T] =
    toAction((c: PContext[V]) => {c.asInstanceOf[PContext[T]].setNodeValue(f(c).value); true})

  implicit def toAction4[T](f: () => Boolean): Rule[T] =
    toAction((c: PContext[_]) => f())

  implicit def toAction5[T](f: () => Unit): Rule[T] =
    toAction((c: PContext[_]) => {f(); true})

  implicit def toAction6[T](f: () => Val[T]): Rule[T] =
    toAction3((c: PContext[_]) => f())

  implicit def toAction7[T](f: String => Boolean): Rule[T] =
    toAction((c: PContext[_]) => f(c.getPrevText))

  implicit def toAction8[T](f: String => Unit): Rule[T] =
    toAction((c: PContext[_]) => {f(c.getPrevText); true})

  implicit def toAction9[T](f: String => Val[T]): Rule[T] =
    toAction3((c: PContext[_]) => f(c.getPrevText))

  implicit def toAction10[V, T](f: Val[V] => Boolean): Rule[T] =
    toAction((c: PContext[V]) => f(Val(c.getPrevValue)))

  implicit def toAction11[V, T](f: Val[V] => Unit): Rule[T] =
    toAction((c: PContext[V]) => {f(Val(c.getPrevValue)); true})

  implicit def toAction12[V, T](f: Val[V] => Val[T]): Rule[T] =
    toAction3((c: PContext[V]) => f(Val(c.getPrevValue)))

}