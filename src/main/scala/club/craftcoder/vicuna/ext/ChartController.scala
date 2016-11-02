package club.craftcoder.vicuna.ext

import java.io.{File, FileWriter}

import club.craftcoder.vicuna.core.Container
import com.ecfront.common.Resp

object ChartController {

  def buildChart(flowCode: String, path: String): Resp[Void] = {
    val graphF = Container.GRAPH_CONTAINER.get(flowCode)
    if (graphF.isDefined) {
      val sb = new StringBuffer()
      sb.append(s"""${graphF.get.startNodeCode}("${graphF.get.startNodeName}[${graphF.get.startNodeCode}]")\r\n""")
      graphF.get.nodes.filterNot(_._1 == graphF.get.startNodeCode).values.foreach {
        node =>
          val n = if (node.childrenNodeCodes.isEmpty) {
            // EOF
            s"""${node.code}("${node.name}[${node.code}]")"""
          } else if (node.childrenNodeCodes.length == 1) {
            s"""${node.code}["${node.name}[${node.code}]"]"""
          } else {
            s"""${node.code}{"${node.name}[${node.code}]"}"""
          }
          sb.append(n + "\r\n")
      }
      Container.TRANSITION_CONTAINER(flowCode).foreach {
        transition =>
          transition._2.foreach {
            tran =>
              sb.append(s"""${transition._1} -- "${if(tran.auto)"[auto]" else ""} ${tran.name}" --> ${tran.toCode}\r\n""")
          }
      }
      val file = new File(path)
      if (!file.exists()) {
        file.createNewFile()
      }
      val writer = new FileWriter(file, false)
      try {
        writer.write(packageHtml(sb.toString))
        writer.close()
      } catch {
        case e: Throwable =>
          writer.close()
          file.delete()
      }
      Resp.success(null)
    } else {
      Resp.notFound(s"not found flow [$flowCode]")
    }

  }

  private def packageHtml(content: String): String = {
    s"""<!DOCTYPE html>
       |<html lang="en">
       |<head>
       |    <meta charset="utf-8">
       |    <title>Vicuna chart</title>
       |    <link rel="stylesheet" href="https://cdn.rawgit.com/knsv/mermaid/6.0.0/dist/mermaid.css">
       |    <script src="https://cdn.rawgit.com/knsv/mermaid/6.0.0/dist/mermaid.min.js"></script>
       |    <script>mermaid.initialize({startOnLoad:true});</script>
       |</head>
       |<body>
       |<div class="mermaid">
       |graph TB
       |$content
       |</div>
       |</body>
       |</html>""".stripMargin
  }

}
