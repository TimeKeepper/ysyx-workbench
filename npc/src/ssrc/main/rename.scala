package rename

import chisel3._
import chisel3.util._
import chisel3.experimental._

package sifive {
  package enterprise {
    package firrtl {
      import _root_.firrtl.annotations._

      case class NestedPrefixModulesAnnotation(
          val target: Target,
          prefix: String,
          inclusive: Boolean
      ) extends SingleTargetAnnotation[Target] {

        def duplicate(n: Target): Annotation =
          NestedPrefixModulesAnnotation(target, prefix, inclusive)
      }
    }

  }

}

object AddPrefix {
  def apply(module: Module, prefix: String, inclusive: Boolean = true) = {
      annotate(new ChiselAnnotation {
        def toFirrtl =
          new sifive.enterprise.firrtl.NestedPrefixModulesAnnotation(module.toTarget, prefix, inclusive)
      })
  }
}
