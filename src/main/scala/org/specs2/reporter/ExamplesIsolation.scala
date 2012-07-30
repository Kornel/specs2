package org.specs2
package reporter

import main.Arguments
import control.LazyParameters._
import execute.Result
import specification._
import specification.Fragments._
import reflect.Classes

/**
 * This trait "isolates" examples by replacing their body with another one, created from a clone of the specification.
 *
 */
trait ExamplesIsolation { self: DefaultSelection =>
  /**
   * This function "clones" the body of each example if the applicable arguments indicate that the specification should
   * be isolated
   */
  protected def isolateExamples(implicit arguments: Arguments) = (fs: Seq[(Fragment, Arguments, SpecName)])=> {
    fs.zipWithIndex.map { fani  =>
      val ((fragment, args, name), index) = fani
      if ((arguments <| args).isolated) {
        fragment match {
          case e @ Example(_,_) if e.isolable => e.copy(body = () => copyBody(name, e.body(), index))
          case a @ Action(_)    if a.isolable => a.copy(action = lazyfy(copyBody(name, a.execute, index)))
          case other                          => other
        }
      } else fragment
    }
  }

  /**
   * @return an Example which body comes from the execution of that example in a brand new instance of the Specification
   */
  protected def copyBody(name: SpecName, body: =>Result, index: Int)(implicit arguments: Arguments) = {
    SpecificationStructure.createSpecificationOption(name.javaClassName).map { specification =>
      val fragments = select(specification.content.fragments)

      def executeStepsBefore(n: Int) =
        fragments.zipWithIndex.collect { case (f, i) if i < n => f }.
          collect(isAStep).
          filter(_.isolable).foreach(_.execute)

      fragments(index) match {
        case e @ Example(_, _) => executeStepsBefore(index); e.execute
        case a @ Action(_)     => executeStepsBefore(index); a.execute
        case other             => body
      }
    }.getOrElse(body)
  }

  /**
  * This function "clones" the body of each example if the applicable arguments indicate that the specification should
  * be isolated
  */
  protected def isolateExamples2(implicit arguments: Arguments) = (fs: Seq[(Fragment, Arguments, SpecName)])=> {
    fs.map { fan  =>
      val (fragment, args, name) = fan
      if ((arguments <| args).isolated) {
        fragment match {
          case e @ Example(_,_) if e.isolable => e.copy(body = () => copyBody2(name, e, e.body()))
          case a @ Action(_)    if a.isolable => a.copy(action = lazyfy(copyBody2(name, a, a.execute)))
          case other                          => other
        }
      } else fragment
    }
  }

  /**
   * @return an Example which body comes from the execution of that example in a brand new instance of the Specification
   */
  protected def copyBody2(name: SpecName, f: Fragment, body: =>Result)(implicit arguments: Arguments) = {
    Classes.tryToCreateObject[Specification](name.javaClassName).map { specification =>
      val fragments = select(specification.fragmentsTo(f))

      def executeStepsBefore = fragments.collect(isAStep).filter(_.isolable).foreach(_.execute)

      fragments.lastOption match {
        case Some(e @ Example(_, _)) => executeStepsBefore; e.execute
        case Some(a @ Action(_))     => executeStepsBefore; a.execute
        case other                   => body
      }
    }.getOrElse(body)
  }
2
}
