package com.itszuvalex.itszulib.configuration.xml

import java.io.File

import scala.xml.{PrettyPrinter, XML}

/**
 * Created by Chris on 12/5/2014.
 */
class XMLLoaderWriter(val file: File) {
  val initialized: Boolean = file.exists
  var xml                  = <xml></xml>
  if (!initialized) {
    file.getParentFile.mkdirs()
    file.createNewFile()
    save()
  }
  else load()

  def save(): Unit = {
    val pp = new PrettyPrinter(80, 2)
    XML.save(file.getPath, XML.loadString(pp.format(xml)), "UTF-8", true, null)
  }

  def load() = xml = XML.loadFile(file)
}
