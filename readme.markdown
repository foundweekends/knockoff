# Knockoff - Markdown in Scala

[![scaladoc](https://javadoc.io/badge2/org.foundweekends/knockoff_3/javadoc.svg)](https://javadoc.io/doc/org.foundweekends/knockoff_3)
[![Maven Central](https://img.shields.io/maven-central/v/org.foundweekends/knockoff_3)](https://central.sonatype.com/artifact/org.foundweekends/knockoff_3)

This is a simple Markdown to object model to XHTML system.

```scala
import knockoff.DefaultDiscounter._

toXHTML(knockoff("""# My Markdown Content """))
```

You can use the blocks returned from the `knockoff` method to do useful things, like fetch the header:

```scala
val blocks = knockoff("""# My markdown""")
blocks.find( _.isInstanceOf[Header] ).map( toText ).getOrElse( "No header" )
```
