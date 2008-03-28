package org.jetbrains.plugins.scala.lang.parser.parsing.top.params

import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IChameleonElementType
import com.intellij.psi.tree.TokenSet

import org.jetbrains.plugins.scala.util.DebugPrint
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.ScalaElementTypes
import org.jetbrains.plugins.scala.lang.lexer.ScalaElementType
import org.jetbrains.plugins.scala.lang.parser.parsing.types.ParamType
import org.jetbrains.plugins.scala.lang.parser.util.ParserUtils
import org.jetbrains.plugins.scala.lang.parser.parsing.types.SimpleType
import org.jetbrains.plugins.scala.lang.parser.bnf.BNF
import org.jetbrains.plugins.scala.lang.parser.parsing.top.template.TemplateBody
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.parser.parsing.nl.LineTerminator
import org.jetbrains.plugins.scala.lang.parser.parsing.base.Modifier
import org.jetbrains.plugins.scala.lang.parser.parsing.expressions._

/** 
* @author Alexander Podkhalyuzin
* Date: 08.02.2008
*/

/*
 * ClassParam ::= {Annotation} [{Modifier} ('val' | 'var')] id [':' ParamType]
 */

object ClassParam {
  def parse(builder: PsiBuilder): Boolean = {
    val classParamMarker = builder.mark
    val annotationsMarker = builder.mark
    while (Annotation.parse(builder)) {}
    annotationsMarker.done(ScalaElementTypes.ANNOTATIONS)
    //parse modifiers
    val modifierMarker = builder.mark
    var isModifier = false
    while (BNF.firstModifier.contains(builder.getTokenType)) {
      Modifier.parse(builder)
      isModifier = true
    }
    modifierMarker.done(ScalaElementTypes.MODIFIERS)
    //Look for var or val
    builder.getTokenType match {
      case ScalaTokenTypes.kVAR |
           ScalaTokenTypes.kVAL => {
        builder.advanceLexer //Let's ate this!
      }
      case _ => {
        if (isModifier) {
          builder error ScalaBundle.message("val.var.expected", new Array[Object](0))
        }
      }
    }
    //Look for identifier
    builder.getTokenType match {
      case ScalaTokenTypes.tIDENTIFIER => {
        builder.advanceLexer //Ate identifier
      }
      case _ => {
        classParamMarker.rollbackTo
        return false
      }
    }
    //Try to parse tale
    builder.getTokenType match {
      case ScalaTokenTypes.tCOLON => {
        val taleMarker = builder.mark
        builder.advanceLexer //Ate ':'
        if (ParamType parse builder) {
          taleMarker.drop
          classParamMarker.done(ScalaElementTypes.CLASS_PARAM)
          return true
        }
        else {
          taleMarker.rollbackTo
          classParamMarker.done(ScalaElementTypes.CLASS_PARAM)
          return true
        }
      }
      case _ => {
        classParamMarker.done(ScalaElementTypes.CLASS_PARAM)
        return true
      }
    }
  }
}