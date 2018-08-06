package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import monaco.editor.IReadOnlyModel
import monaco.languages.Location
import monaco.languages.ReferenceContext
import monaco.languages.ReferenceProvider
import monaco.CancellationToken
import monaco.Position
import MetadocEnrichments._

class ScalaReferenceProvider(index: MetadocSemanticdbIndex, fetch: MetadocFetch)
    extends ReferenceProvider {
  val service = new MetadocTextModelService(fetch)

  override def provideReferences(
      model: IReadOnlyModel,
      position: Position,
      context: ReferenceContext,
      token: CancellationToken
  ) = {
    val offset = model.getOffsetAt(position).toInt
    for {
      sym <- index.fetchSymbol(offset)
      locations <- Future.sequence {
        val references = sym.map(_.references).getOrElse(Map.empty)
        references.map {
          case (filename, ranges) =>
            // Create the model for each reference. A reference can come from
            // another file, and we need that file's model in order to get
            // correct range selection.

              service.modelDocument(createUri(filename))
              .map {
                case MetadocMonacoDocument(_, model) =>
                  ranges.ranges.map { r =>
                    model.`object`.textEditorModel.resolveLocation(
                      r.toPosition(filename)
                    )
                  }
              }
        }
      }
    } yield {
      js.Array[Location](locations.flatten.toSeq: _*)
    }
  }.toMonacoThenable
}
