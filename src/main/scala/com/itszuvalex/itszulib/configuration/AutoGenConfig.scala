package com.itszuvalex.itszulib.configuration

import java.io.File

import net.minecraftforge.common.config.Configuration
import net.minecraftforge.common.config.Configuration._

/**
  * Created by Itszuvalex on 12/31/14.
  */
class AutoGenConfig(val massive: String, val major: String, val minor: String, val build: String) {
  private val CATEGORY = "Configuration Regeneration"

  var config: Configuration = null

  def load(configuration: Configuration): Unit = {
    config = configuration
    config.load()
  }

  def shouldRegenFile(file: File) = get(config.getBoolean("Always Regenerate " + file.getName, CATEGORY + CATEGORY_SPLITTER + file.getName, false,
                                                          "Always regenerate " + file.getName + ". When true, this overrides should regenerate.")
                                        || (((regenOnMassiveChange && massiveVersionChanged(file)) ||
                                             (regenOnMajorChange && majorVersionChanged(file)) ||
                                             (regenOnMinorChange && minorVersionChanged(file)) ||
                                             (regenOnBuildChange && buildVersionChanged(file))) &&
                                            get(config.getBoolean("Should Regenerate " + file.getName, CATEGORY + CATEGORY_SPLITTER + file.getName, true,
                                                                  "Set to false to prevent regeneration for this file."))) ||
                                        shouldRegenAlways)

  def shouldRegenAlways = get(config.getBoolean("Always Regenerate Configs", CATEGORY, false,
                                                "When true, will always regenerate all files in the autogen folder on startup." +
                                                "Overrides ALL per-file regeneration settings.")
                             )

  def regenOnMassiveChange = get(config.getBoolean("Massive", CATEGORY, true, "Regenerate when Massive version is different.")
                                 || shouldRegenAlways)

  def regenOnMajorChange = get(config.getBoolean("Major", CATEGORY, true, "Regenerate when Major version is different.")
                               || shouldRegenAlways)

  def regenOnMinorChange = get(config.getBoolean("Minor", CATEGORY, true, "Regenerate when Minor version is different.")
                               || shouldRegenAlways)

  def regenOnBuildChange = get(config.getBoolean("Build", CATEGORY, false, "Regenerate when Build version is different.")
                               || shouldRegenAlways)

  def markFileRegenerated(file: File) = {
    if (massiveVersionChanged(file)) {
      config.get(CATEGORY + CATEGORY_SPLITTER + file.getName + CATEGORY_SPLITTER + "Version", "Massive", massive).set(massive)
    }
    if (majorVersionChanged(file)) {
      config.get(CATEGORY + CATEGORY_SPLITTER + file.getName + CATEGORY_SPLITTER + "Version", "Major", major).set(major)
    }
    if (minorVersionChanged(file)) {
      config.get(CATEGORY + CATEGORY_SPLITTER + file.getName + CATEGORY_SPLITTER + "Version", "Minor", minor).set(minor)
    }
    if (buildVersionChanged(file)) {
      config.get(CATEGORY + CATEGORY_SPLITTER + file.getName + CATEGORY_SPLITTER + "Version", "Build", build).set(build)
    }
  }

  def massiveVersionChanged(file: File) = !getMassiveVersion(file).equalsIgnoreCase(massive)

  def getMassiveVersion(file: File) = get(config.getString("Massive", CATEGORY + CATEGORY_SPLITTER + file.getName + CATEGORY_SPLITTER + "Version", massive, ""))

  def majorVersionChanged(file: File) = !getMajorVersion(file).equalsIgnoreCase(major)

  def getMajorVersion(file: File) = get(config.getString("Major", CATEGORY + CATEGORY_SPLITTER + file.getName + CATEGORY_SPLITTER + "Version", major, ""))

  def minorVersionChanged(file: File) = !getMinorVersion(file).equalsIgnoreCase(minor)

  def getMinorVersion(file: File) = get(config.getString("Minor", CATEGORY + CATEGORY_SPLITTER + file.getName + CATEGORY_SPLITTER + "Version", minor, ""))

  def get[T](value: T): T = {
    if (config.hasChanged) config.save()
    value
  }

  def buildVersionChanged(file: File) = !getBuildNumber(file).equalsIgnoreCase(build)

  def getBuildNumber(file: File) = get(config.getString("Build", CATEGORY + CATEGORY_SPLITTER + file.getName + CATEGORY_SPLITTER + "Version", build, ""))

}
