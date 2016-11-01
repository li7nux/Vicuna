package club.craftcoder.vicuna.ext

import java.io.{File, FileWriter}

import club.craftcoder.vicuna.core.Container
import com.ecfront.common.Resp

object ChartController {

  def buildChart(flowCode: String, path: String): Resp[Void] = {
    val graphF = Container.GRAPH_CONTAINER.get(flowCode)
    if (graphF.isDefined) {
      val sb = new StringBuffer()
      sb.append(s"""${graphF.get.startNodeCode}=>start: ${graphF.get.startNodeName}(${graphF.get.startNodeCode})\r\n""")
      graphF.get.nodes.filterNot(_._1 == graphF.get.startNodeCode).values.foreach {
        node =>
          val n = if (node.childrenNodeCodes.isEmpty) {
            s"""${node.code}=>end: ${node.name}(${node.code})"""
          } else if (node.childrenNodeCodes.length == 1) {
            s"""${node.code}=>operation: ${node.name}(${node.code})"""
          } else {
            s"""${node.code}=>condition: ${node.name}(${node.code})"""
          }
          sb.append(n + "\r\n")
      }
      sb.append("\r\n")
      Container.TRANSITION_CONTAINER(flowCode).foreach {
        transition =>
          transition._2.foreach {
            tran =>
              sb.append(s"""${transition._1}([${tran.auto}]${if (tran.condition != null) tran.condition.toString() else ""})->${tran.toCode}\r\n""")
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
    s"""
       |<!DOCTYPE html>
       |<html lang="en">
       |<head>
       |    <meta charset="utf-8">
       |    <title>Vicuna chart</title>
       |    <script src="http://cdnjs.cloudflare.com/ajax/libs/raphael/2.1.0/raphael-min.js"></script>
       |    <script src="http://cdnjs.cloudflare.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>
       |    <script src="http://flowchart.js.org/flowchart-latest.js"></script>
       |    <script>
       |        window.onload = function () {
       |            var chart = flowchart.parse(document.getElementById("code").value);
       |            chart.drawSVG('canvas', {
       |                // 'x': 30,
       |                // 'y': 50,
       |                'line-width': 3,
       |                'line-length': 50,
       |                'text-margin': 10,
       |                'font-size': 14,
       |                'font': 'normal',
       |                'font-family': 'Helvetica',
       |                'font-weight': 'normal',
       |                'font-color': 'black',
       |                'line-color': 'black',
       |                'element-color': 'black',
       |                'fill': 'white',
       |                'yes-text': 'yes',
       |                'no-text': 'no',
       |                'arrow-end': 'block',
       |                'scale': 1,
       |                'symbols': {
       |                    'start': {
       |                        'font-color': 'red',
       |                        'element-color': 'green',
       |                        'fill': 'yellow'
       |                    },
       |                    'end': {
       |                        'class': 'end-element'
       |                    }
       |                },
       |                'flowstate': {
       |                    'past': {'fill': '#CCCCCC', 'font-size': 12},
       |                    'current': {'fill': 'yellow', 'font-color': 'red', 'font-weight': 'bold'},
       |                    'future': {'fill': '#FFFF99'},
       |                    'request': {'fill': 'blue'},
       |                    'invalid': {'fill': '#444444'},
       |                    'approved': {'fill': '#58C4A3', 'font-size': 12, 'yes-text': 'APPROVED', 'no-text': 'n/a'},
       |                    'rejected': {'fill': '#C45879', 'font-size': 12, 'yes-text': 'n/a', 'no-text': 'REJECTED'}
       |                }
       |            });
       |        };
       |    </script>
       |</head>
       |<body>
       |<div>
       |    <textarea id="code" style="width: 100%;display:none;">
       |$content
       |    </textarea>
       |</div>
       |<div id="canvas"></div>
       |</body>
       |</html>
     """.stripMargin
  }

}
