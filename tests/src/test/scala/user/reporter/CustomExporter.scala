package user.reporter

import org.specs2.reporter.Exporter
import org.specs2.main.Arguments
import org.specs2.specification.ExecutingSpecification

class CustomExporter extends Exporter {
  def export(implicit args: Arguments) = (s: ExecutingSpecification) => s.execute
}


