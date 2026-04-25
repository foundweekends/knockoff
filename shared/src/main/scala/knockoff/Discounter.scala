/*

## Converting Markdown to HTML ##

The `Discounter` is what grabs the "knockoff" of a markdown string. For simple usage,
you can use the `DefaultDiscounter` object.

    import knockoff.DefaultDiscounter._
    toXHTML( knockoff( markdownString ) )

 */
package knockoff

import scala.collection.mutable.ListBuffer
import scala.util.parsing.input.Position
import scala.util.parsing.input.CharSequenceReader

trait Discounter extends ChunkStreamFactory with TextWriter {

  /** Parses and returns our best guess at the sequence of blocks. It will
      never fail, just log all suspicious things. */
  def knockoff(source: java.lang.CharSequence): collection.Seq[Block] = {

    val chunks = createChunkStream(new CharSequenceReader(source, 0))

    // These next lines are really ugly because I couldn't figure out a nice
    // way to match a tuple argument (thank you erasure!)
    val linkDefinitions = chunks.flatMap { case ((chunk, pos)) =>
      chunk match {
        case c: LinkDefinitionChunk =>
          c :: Nil
        case _ =>
          Nil
      }
    }

    val convert = createSpanConverter(linkDefinitions)

    val spanned = chunks.map { chunkAndPos =>
      (chunkAndPos._1, convert(chunkAndPos._1), chunkAndPos._2)
    }

    combine(spanned.toList, new ListBuffer)
  }

  def createSpanConverter(linkDefinitions: collection.Seq[LinkDefinitionChunk]): SpanConverter =
    new SpanConverter(linkDefinitions)

  /** Consume input and append the right thing to the output until empty. The
      Chunk itself determines the "right thing to do". All chunks only know what
      has come before itself, by peering into the output. (It shouldn't matter
      what comes next...) */
  private def combine(
    input: List[(Chunk, collection.Seq[Span], Position)],
    output: ListBuffer[Block]
  ): collection.Seq[Block] = {
    input match {
      case h :: t =>
        h._1.appendNewBlock(output, t, h._2, h._3, this)
        combine(input.tail, output)
      case Nil =>
        output
    }
  }
}
