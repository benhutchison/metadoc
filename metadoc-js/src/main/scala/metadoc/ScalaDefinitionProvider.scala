package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import monaco.editor.IReadOnlyModel
import monaco.languages.DefinitionProvider
import monaco.languages.Location
import monaco.CancellationToken
import monaco.Position

class ScalaDefinitionProvider(index: MetadocSemanticdbIndex, fetch: MetadocFetch)
    extends DefinitionProvider {
  val service = new MetadocTextModelService(fetch)

  override def provideDefinition(
      model: IReadOnlyModel,
      position: Position,
      token: CancellationToken
  ) = {
    val offset = model.getOffsetAt(position).toInt
    def empty = Future.successful(js.Array[Location]())
    for {
      symbol <- index.fetchSymbol(offset)
      locations <- {
        symbol
          .map(_.definition)
          .fold(empty) {
            case Some(defn) =>
              for {
                model <- service.modelReference(defn.filename)
              } yield {
                val location =
                  model.`object`.textEditorModel.resolveLocation(defn)
                js.Array[Location](location)
              }
            case None => empty
          }
      }
    } yield locations
  }.toMonacoThenable
}
