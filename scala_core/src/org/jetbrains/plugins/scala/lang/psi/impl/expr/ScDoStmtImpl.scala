package org.jetbrains.plugins.scala.lang.psi.impl.expr

import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.ScalaElementTypes
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElementImpl






import com.intellij.psi.tree.TokenSet
import com.intellij.lang.ASTNode
import com.intellij.psi.tree.IElementType;
import com.intellij.psi._

import org.jetbrains.annotations._

import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.icons.Icons


import org.jetbrains.plugins.scala.lang.psi.api.expr._

/** 
* @author Alexander Podkhalyuzin
* Date: 06.03.2008
*/

class ScDoStmtImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScDoStmt {
  override def toString: String = "DoStatement"

  override def isCondition = (e: PsiElement) => {
    e.isInstanceOf[ScExpression] &&
    {
      val parent = e.getParent
      val last = for (val child <- parent.getChildren; child.isInstanceOf[ScExpression]) child
      parent == last
    }
  }
}