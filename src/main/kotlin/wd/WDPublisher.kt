package wd

import org.apache.logging.log4j.LogManager
import org.wikidata.wdtk.datamodel.helpers.Datamodel
import org.wikidata.wdtk.datamodel.helpers.Datamodel.makeItemIdValue
import org.wikidata.wdtk.datamodel.helpers.Datamodel.makePropertyIdValue
import org.wikidata.wdtk.datamodel.helpers.ItemDocumentBuilder
import org.wikidata.wdtk.datamodel.helpers.ReferenceBuilder
import org.wikidata.wdtk.datamodel.helpers.StatementBuilder
import org.wikidata.wdtk.datamodel.interfaces.*
import org.wikidata.wdtk.util.WebResourceFetcherImpl
import org.wikidata.wdtk.wikibaseapi.ApiConnection
import org.wikidata.wdtk.wikibaseapi.BasicApiConnection
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor
import java.net.ConnectException

const val IRI_TestInstance = "http://www.test.wikidata.org/entity/"

class EnvironmentVariableError(message: String) : Exception(message)
class InternalError(message: String) : Exception(message)

interface InstanceItems {
    enum class Properties(val id: PropertyIdValue)
    enum class Items(val id: ItemIdValue)
    val siteIri: String
    val inChIKey: PropertyIdValue
    val inChI: PropertyIdValue
    val isomericSMILES: PropertyIdValue
    val canonicalSMILES: PropertyIdValue
    val pcId: PropertyIdValue
    val chemicalFormula: PropertyIdValue
    val instanceOf: PropertyIdValue
    val chemicalCompound: ItemIdValue
}

object TestInstanceItems : InstanceItems {
    override val siteIri = "http://www.test.wikidata.org/entity/"
    override val inChIKey = makePropertyIdValue("P95461", siteIri)
    override val inChI = makePropertyIdValue("P95462", siteIri)
    override val isomericSMILES = makePropertyIdValue("P95463", siteIri)
    override val canonicalSMILES = makePropertyIdValue("P95466", siteIri)
    override val pcId = makePropertyIdValue("P95464", siteIri)
    override val chemicalFormula = makePropertyIdValue("P95465", siteIri)
    override val instanceOf = makePropertyIdValue("P82", siteIri)
    override val chemicalCompound = makeItemIdValue("Q212525", IRI_TestInstance)
}

// TODO: Real instance
// ?id wdt:P31   wd:Q11173;
//     wdt:P235  "InChIKey";
//     wdt:P234  "InChI";
//     wdt:P2017 "SMILES_isomeric";
//     wdt:P664  "PCID";
//     wdt:P274  "Hill Chemical Formula".


class WDPublisher {
    val userAgent = "Wikidata Toolkit EditOnlineDataExample"
    val siteIri = "http://www.test.wikidata.org/entity/"
    val sitePageURL = "https://test.wikidata.org/w/index.php?title="
    val logger = LogManager.getLogger(this::class.java)
    private var user: String? = null
    private var password: String? = null
    private var connection: ApiConnection? = null
    private var editor: WikibaseDataEditor? = null

    init {
        user = System.getenv("WIKIDATA_USER")
            ?: throw EnvironmentVariableError("Missing environment variable WIKIDATA_USER")
        password = System.getenv("WIKIDATA_PASSWORD")
            ?: throw EnvironmentVariableError("Missing environment variable WIKIDATA_PASSWORD")
    }

    fun connect() {
        connection = BasicApiConnection.getTestWikidataApiConnection()
        connection?.login(user, password) ?: throw ConnectException("Impossible to connect to the WikiData instance.")
        editor = WikibaseDataEditor(connection, siteIri)
        require(connection?.isLoggedIn ?: false) { "Impossible to login in the instance" }
    }

    fun disconnect() = connection?.logout()

    fun publish(itemDocument: ItemDocument, summary: String) {
        require(connection != null) { "You need to connect first" }
        require(editor != null) { "The editor should exist, you connection likely failed and we didn't catch that" }
        WebResourceFetcherImpl
            .setUserAgent(userAgent)

        val newItemDocument: ItemDocument = editor?.createItemDocument(
            itemDocument,
            summary, null
        ) ?: throw InternalError("There is no editor anymore")

        val newItemId = newItemDocument.entityId
        logger.info("Successfully created the item: ${newItemId.id}")
        logger.info("you can access it at $sitePageURL${newItemId.id}")
    }
}

/**
 * Type safe builder or DSL
 */

fun newReference(f: (ReferenceBuilder) -> Unit): Reference {
    val reference = ReferenceBuilder.newInstance()
    reference.apply(f)
    return reference.build()
}

fun ReferenceBuilder.propertyValue(property: PropertyIdValue, value: String) =
    this.propertyValue(property, Datamodel.makeStringValue(value))

fun ReferenceBuilder.propertyValue(property: PropertyIdValue, value: Value) {
    this.withPropertyValue(
        property,
        value
    )
}

fun StatementBuilder.reference(reference: Reference) {
    this.reference(reference)
}

fun newDocument(name: String, f: ItemDocumentBuilder.()->Unit): ItemDocument {
    val builder = ItemDocumentBuilder.forItemId(ItemIdValue.NULL)
        .withLabel(name, "en")

    builder.apply(f)

    return builder.build()
}

fun ItemDocumentBuilder.statement(statement: Statement) {
    this.withStatement(statement)
}


fun ItemDocumentBuilder.statement(property: PropertyIdValue, value: String, f: (StatementBuilder) -> Unit = {}) =
    this.withStatement(newStatement(property, Datamodel.makeStringValue(value), f))

fun ItemDocumentBuilder.statement(property: PropertyIdValue, value: Value, f: (StatementBuilder) -> Unit = {}) =
    this.withStatement(newStatement(property, value, f))

fun newStatement(property: PropertyIdValue, value: Value, f: (StatementBuilder) -> Unit = {}): Statement {
    val statement = StatementBuilder.forSubjectAndProperty(ItemIdValue.NULL, property)
    statement.withValue(value)
    statement.apply(f)
    return statement.build()
}