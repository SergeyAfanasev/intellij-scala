package org.jetbrains.plugins.scala
package lang
package psi
package api
package toplevel
package typedef

import com.intellij.navigation.NavigationItem
import com.intellij.openapi.util.Iconable
import com.intellij.psi._
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.plugins.scala.lang.psi.api.statements._
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.SyntheticMembersInjector
import org.jetbrains.plugins.scala.lang.psi.types.PhysicalSignature

/**
 * @author AlexanderPodkhalyuzin
 */

trait ScTypeDefinition extends ScTemplateDefinition with ScMember
    with NavigationItem with PsiClass with ScTypeParametersOwner with Iconable with ScDocCommentOwner
    with ScAnnotationsHolder with ScCommentOwner {

  def isCase: Boolean = false

  def isObject: Boolean = false

  def isTopLevel = !parentsInFile.exists(_.isInstanceOf[ScTypeDefinition])

  def getPath: String = {
    val qualName = qualifiedName
    val index = qualName.lastIndexOf('.')
    if (index < 0) "" else qualName.substring(0, index)
  }

  def getQualifiedNameForDebugger: String

  /**
   * Qualified name stops on outer Class level.
   */
  def getTruncedQualifiedName: String

  def signaturesByName(name: String): Seq[PhysicalSignature]

  def isPackageObject = false

  override def accept(visitor: ScalaElementVisitor) {
    visitor.visitTypeDefinition(this)
  }

  def getObjectClassOrTraitToken: PsiElement

  def getSourceMirrorClass: PsiClass

  override def isEquivalentTo(another: PsiElement): Boolean = {
    PsiClassImplUtil.isClassEquivalentTo(this, another)
  }

  @volatile
  private var fakeModule: Option[ScObject] = null
  @volatile
  private var fakeModuleModCount: Long = -1L

  def fakeCompanionModule: Option[ScObject] = {
    if (this.isInstanceOf[ScObject]) return None
    val baseCompanion = ScalaPsiUtil.getBaseCompanionModule(this)
    baseCompanion match {
      case Some(td: ScObject) => return None
      case _ if !isCase && !SyntheticMembersInjector.needsCompanion(this) => return None
      case _ =>
    }
    def calc(clazz: ScTypeDefinition): Option[ScObject] = {
      val accessModifier = clazz.getModifierList.accessModifier.fold("")(_.modifierFormattedText + " ")
      val objText = clazz match {
        case clazz: ScClass if clazz.isCase =>
          val texts = clazz.getSyntheticMethodsText

          val extendsText = {
            try {
              if (typeParameters.isEmpty && clazz.constructor.get.effectiveParameterClauses.length == 1) {
                val typeElementText =
                  clazz.constructor.get.effectiveParameterClauses.map {
                    clause =>
                      clause.effectiveParameters.map(parameter => {
                        val parameterText = parameter.typeElement.fold("_root_.scala.Nothing")(_.getText)
                        if (parameter.isRepeatedParameter) s"_root_.scala.Seq[$parameterText]"
                        else parameterText
                      }).mkString("(", ", ", ")")
                  }.mkString("(", " => ", s" => $name)")
                val typeElement = ScalaPsiElementFactory.createTypeElementFromText(typeElementText, getManager)
                s" extends ${typeElement.getText}"
              } else {
                ""
              }
            } catch {
              case e: Exception => ""
            }
          }

          s"""${accessModifier}object ${clazz.name}$extendsText{
             |  ${texts.mkString("\n  ")}
             |}""".stripMargin
        case _ =>
          s"""${accessModifier}object ${clazz.name} {
             |  //Generated synthetic object
             |}""".stripMargin
      }


      val next = ScalaPsiUtil.getNextStubOrPsiElement(clazz)
      val obj: ScObject =
        ScalaPsiElementFactory.createObjectWithContext(objText, clazz.getParent, if (next != null) next else clazz)
      import org.jetbrains.plugins.scala.extensions._
      val objOption: Option[ScObject] = obj.toOption
      objOption.foreach { (obj: ScObject) =>
        obj.setSyntheticObject()
        obj.members.foreach {
          case s: ScFunctionDefinition =>
            s.setSynthetic(clazz) // So we find the `apply` method in ScalaPsiUti.syntheticParamForParam
            clazz match {
              case clazz: ScClass if clazz.isCase =>
                s.syntheticCaseClass = Some(clazz)
              case _ =>
            }
          case _ =>
        }
      }
      objOption
    }
    val modCount = PsiModificationTracker.SERVICE.getInstance(getProject).getOutOfCodeBlockModificationCount
    if (fakeModule != null && modCount == fakeModuleModCount) return fakeModule
    val res = calc(this)
    synchronized {
      if (fakeModule != null && modCount == fakeModuleModCount) return fakeModule
      fakeModule = res
      fakeModuleModCount = modCount
    }
    res
  }
}