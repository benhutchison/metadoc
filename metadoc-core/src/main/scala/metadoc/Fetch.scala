package metadoc

case class FetchRequest(path: String)

case class FetchResponse(path: String, optData: Option[Array[Byte]])