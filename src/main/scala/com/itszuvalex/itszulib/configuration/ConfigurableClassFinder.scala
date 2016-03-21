package com.itszuvalex.itszulib.configuration

import com.google.common.reflect.ClassPath
import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.api.core.Configurable
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.Side
import net.minecraftforge.common.config.Configuration
import org.apache.logging.log4j.Level

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


/**
  * Created by Christopher Harris (Itszuvalex) on 10/10/14.
  */
class ConfigurableClassFinder(val classPackage: String, val configKey: String = "Class Constants") {
  private val configurableClasses    = new ArrayBuffer[Class[_]]
  private val configurableSingletons = new mutable.HashMap[AnyRef, Class[_]]

  def loadClassConstants(configuration: Configuration) = {
    configurableClasses.foreach(clazz => ConfigHelper.loadClassFromConfig(configuration, configKey, clazz.getSimpleName, clazz))
    configurableSingletons.foreach(pair => ConfigHelper.loadClassInstanceFromConfig(configuration, configKey, pair._2.getSimpleName.subSequence(0, pair._2.getSimpleName.length - 1).toString, pair._2, pair._1))
  }

  def registerConfigurableClasses() {
    ItszuLib.logger.log(Level.INFO, "Finding all configurable classes for registration.")
    val classes = ClassPath.from(getClass.getClassLoader).getTopLevelClassesRecursive(classPackage)
    classes.foreach(info => {
      try {
        val side = FMLCommonHandler.instance().getEffectiveSide
        val clientPackage = info.getResourceName.toLowerCase.matches(".*client.*") ||
                            info.getResourceName.toLowerCase.matches(".*gui.*") ||
                            info.getResourceName.toLowerCase.matches(".*render.*") ||
                            info.getResourceName.toLowerCase.matches(".*fx.*") ||
                            info.getResourceName.toLowerCase.matches(".*model.*")
        if (!clientPackage || (clientPackage && (side == Side.CLIENT))) {
          val clazz = Class.forName(info.getName)
          try {
            if (clazz.getAnnotation(classOf[Configurable]) != null) {
              try {
                val compclazz = Class.forName(info.getName + "$")
                val inst = compclazz.getField("MODULE$").get(null)
                if (inst != null) {
                  configurableSingletons.put(inst, compclazz)
                  ItszuLib.logger.log(Level.INFO, "Registered " + clazz.getSimpleName + " as configurable singleton.")
                }
              }
              catch {
                case e: ClassNotFoundException =>
                  registerConfigurableClass(clazz)
                  ItszuLib.logger.log(Level.INFO, "Registered " + clazz.getSimpleName + " as configurable.")
                case e: Exception =>
              }
            }
          }
          catch {
            case e: Exception =>
          }
        }
      }
      catch {
        case e: RuntimeException =>
        case e: ClassNotFoundException =>
        case e: NoClassDefFoundError =>
      }
    })
    ItszuLib.logger.log(Level.INFO, "Registered " + configurableClasses.length + " configurable classes.")
    ItszuLib.logger.log(Level.INFO, "Registered " + configurableSingletons.size + " configurable singletons.")
  }

  /**
    * @param clazz Class to load all @Configurable annotated public/private fields from.
    * @return True if class successfully added.
    */
  def registerConfigurableClass(clazz: Class[_]) = configurableClasses.append(clazz)

}
