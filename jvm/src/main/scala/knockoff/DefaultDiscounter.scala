/*

## Converting Markdown to HTML ##

The `Discounter` is what grabs the "knockoff" of a markdown string. For simple usage,
you can use the `DefaultDiscounter` object.

    import knockoff.DefaultDiscounter._
    toXHTML( knockoff( markdownString ) )

 */
package knockoff

import java.io.File

/*

### The Default Discounter

Provides an object along with a main method for the "good enough to script" kind of
usage. Note that a major aim of this discounter is to mimic the usage of
`Markdown.pl`.

    Markdown.pl [ −−html4tags ] [ −−version ] [ −shortversion ] [ file ... ]

The `--html4tags` argument will just do nothing, but not be processed as a file.


 */

object DefaultDiscounter extends Discounter with XHTMLWriter {
  def main(args: Array[String]): Unit = try {
    if (args.contains("--version")) {
      Console.err.print("DefaultDiscounter ")
    }
    if (args.contains("--version") || args.contains("-shortversion")) {
      Console.err.println(KnockoffBuildInfo.version)
      return
    }

    if (args.isEmpty) {
      val sb = new StringBuilder
      var line: String = Console.in.readLine()
      while (line != null) {
        sb.append(line)
        line = Console.in.readLine()
      }
      println(toXHTML(knockoff(sb.toString)).toString)
    } else {
      args.filter(_ != "--html4tags").foreach { fileName =>
        println(toXHTML(knockoff(readText(fileName))).toString)
      }
    }
  } catch {
    case th: Throwable => {
      th.printStackTrace(Console.err)
    }
  }

  private def readText(fileName: String): String =
    io.Source.fromFile(new File(fileName)).mkString("")
}
