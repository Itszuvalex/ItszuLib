package com.itszuvalex.itszulib.render

import java.io.{BufferedReader, InputStreamReader}
import java.nio.{FloatBuffer, IntBuffer}
import java.util

import com.itszuvalex.itszulib.ItszuLib
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.renderer.OpenGlHelper
import org.apache.logging.log4j.Level
import org.lwjgl.opengl.{ARBFragmentShader, ARBShaderObjects, ARBVertexShader, GL11}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
  * Adapted from Vazkii's ShaderHelper class.
  *
  * https://github.com/Vazkii/Botania/blob/master/src/main/java/vazkii/botania/client/core/helper/ShaderHelper.java
  */
@SideOnly(Side.CLIENT) object ShaderUtils {
  final val VERT = ARBVertexShader.GL_VERTEX_SHADER_ARB
  final val FRAG = ARBFragmentShader.GL_FRAGMENT_SHADER_ARB

  private val shaderParameterMap = new util.TreeMap[Int, util.Map[String, (Unit) => Any]]().asScala

  var portal = 0

  /*
  * Test
   */
  def init(): Unit = {
    if (!ShaderUtils.canUseShaders) return

    portal = ShaderUtils.loadShader("/assets/itszulib/shader/portal.vert", "/assets/itszulib/shader/portal.frag")
  }

  def loadShader(vertFile: String, fragFile: String) = createProgram(vertFile, fragFile)

  private def createProgram(vert: String, frag: String): Int = {
    var vertId = 0
    var fragId = 0
    var program = 0
    if (vert != null) vertId = createShader(vert, VERT)
    if (frag != null) fragId = createShader(frag, FRAG)
    program = ARBShaderObjects.glCreateProgramObjectARB()
    if (program == 0) return 0
    if (vert != null) ARBShaderObjects.glAttachObjectARB(program, vertId)
    if (frag != null) ARBShaderObjects.glAttachObjectARB(program, fragId)
    ARBShaderObjects.glLinkProgramARB(program)
    if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
      ItszuLib.logger.log(Level.ERROR, getLogInfo(program))
      return 0
    }
    ARBShaderObjects.glValidateProgramARB(program)
    if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE) {
      ItszuLib.logger.log(Level.ERROR, getLogInfo(program))
      return 0
    }
    program
  }

  private def createShader(fileName: String, shaderType: Int): Int = {
    var shader = 0
    try {
      shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType)

      if (shader == 0) return 0

      ARBShaderObjects.glShaderSourceARB(shader, readFileAsString(fileName))
      ARBShaderObjects.glCompileShaderARB(shader)

      if (ARBShaderObjects.glGetObjectParameteriARB(shader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
        throw new RuntimeException("Error creating shader: " + getLogInfo(shader))

      shader
    } catch {
      case e: Exception =>
        ARBShaderObjects.glDeleteObjectARB(shader)
        e.printStackTrace()
        -1
    }
  }

  @throws[Exception]
  private def readFileAsString(filename: String): String = {
    val source = new StringBuilder
    val in = getClass.getResourceAsStream(filename)
    var e: Exception = null
    var reader: BufferedReader = null
    try {
      reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))

      var innere: Exception = null
      try {
        var line: String = null
        while ( {line = reader.readLine(); line} != null) {
          source.append(line).append('\n')
        }
      }
      catch {case ex: Exception => e = ex}
      finally {
        try {reader.close()}
        catch {case ex: Exception => if (innere == null) innere = ex else ex.printStackTrace()}
      }
      if (innere != null) throw innere
    } catch {case ex: Exception => e = ex}
    finally {
      try in.close()
      catch {case ex: Exception => if (e == null) e = ex else ex.printStackTrace()}
      if (e != null) throw e
    }
    source.toString()
  }

  private def getLogInfo(obj: Int): String = ARBShaderObjects.glGetInfoLogARB(obj, ARBShaderObjects.glGetObjectParameteriARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB))

  def canUseShaders = OpenGlHelper.shadersSupported

  def registerShaderAdditionalParams(shader: Int, values: util.Map[String, (Unit) => Any]): Unit = {
    shaderParameterMap(shader) = values
  }

  def bindShader(shader: Int) {
    if (!ShaderUtils.canUseShaders) return

    try {
      ARBShaderObjects.glUseProgramObjectARB(shader)

      shaderParameterMap.get(shader) match {
        case Some(a) => a.foreach { case (name, vfun) =>
          val loc = ARBShaderObjects.glGetUniformLocationARB(shader, name);
          vfun(Unit) match {
            case i: Int => ARBShaderObjects.glUniform1iARB(loc, i)
            case f: Float => ARBShaderObjects.glUniform1fARB(loc, f)
            case ib: IntBuffer => ARBShaderObjects.glUniform1ARB(loc, ib)
            case fb: FloatBuffer => ARBShaderObjects.glUniform1ARB(loc, fb)
            case _ =>
          }
                                  }
        case None =>
      }
    } catch {case _: Throwable => releaseShader()}
  }

  def releaseShader() = bindShader(0)

}
