/*
 * This Work is in the public domain and is provided on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied,
 * including, without limitation, any warranties or conditions of TITLE,
 * NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A PARTICULAR PURPOSE.
 * You are solely responsible for determining the appropriateness of using
 * this Work and assume any risks associated with your use of this Work.
 *
 * This Work includes contributions authored by David E. Jones, not as a
 * "work for hire", who hereby disclaims any copyright to the same.
 */
package org.moqui.entity;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;

/** The main interface for general database operations in Moqui. */
public interface EntityFacade {

    /** Get a EntityConditionFactory object that can be used to create and assemble conditions used for finds.
     *
     * @return The facade's active EntityConditionFactory object.
     */
    EntityConditionFactory getConditionFactory();

    /** Creates a Entity in the form of a EntityValue without persisting it
     *
     * @param entityName The name of the entity to make a value object for.
     * @return EntityValue for the named entity. 
     */
    EntityValue makeValue(String entityName);
    
    /** Create an EntityFind object that can be used to specify additional options, and then to execute one or more
     * finds (queries).
     * 
     * @param entityName The Name of the Entity as defined in the entity XML file, can be null.
     * @return An EntityFind object.
     */
    EntityFind makeFind(String entityName);

    /** Do a database query with the given SQL and return the results as an EntityList for the given entity and with
     * selected columns mapped to the listed fields.
     *
     * @param sql The actual SQL to run.
     * @param sqlParameterList Parameter values that correspond with any question marks (?) in the SQL.
     * @param entityName Name of the entity to map the results to.
     * @param fieldList List of entity field names in order that they match columns selected in the query.
     * @return EntityListIterator with results of query.
     */
    EntityListIterator sqlFind(String sql, List<Object> sqlParameterList, String entityName, List<String> fieldList);

    /** Find and assemble data documents represented by a Map that can be easily turned into a JSON document. This is
     * used for searching by the Data Search feature and for data feeds to other systems with the Data Feed feature.
     *
     * @param dataDocumentId Used to look up the DataDocument and related records (DataDocument* entities).
     * @param condition An optional condition to AND with from/thru updated timestamps and any DataDocumentCondition
     *                  records associated with the DataDocument.
     * @param fromUpdateStamp The lastUpdatedStamp on at least one entity selected must be after (>=) this Timestamp.
     * @param thruUpdatedStamp The lastUpdatedStamp on at least one entity selected must be before (<) this Timestamp.
     * @return List of Maps with these entries:
     *      - _index = DataDocument.indexName
     *      - _type = dataDocumentId
     *      - _id = pk field values from primary entity, underscore separated
     *      - _timestamp = timestamp when the document was created
     *      - Map for primary entity (with primaryEntityName as key)
     *      - nested List of Maps for each related entity from DataDocumentField records with aliased fields
     *          (with relationship name as key)
     */
    List<Map> getDataDocuments(String dataDocumentId, EntityCondition condition, Timestamp fromUpdateStamp,
                                Timestamp thruUpdatedStamp);

    /** Get the next guaranteed unique seq id from the sequence with the given sequence name;
     * if the named sequence doesn't exist, it will be created.
     *
     * @param seqName The name of the sequence to get the next seq id from
     * @param staggerMax The maximum amount to stagger the sequenced ID, if 1 the sequence will be incremented by 1,
     *     otherwise the current sequence ID will be incremented by a value between 1 and staggerMax
     * @param bankSize The size of the "bank" of values to get from the database (defaults to 1)
     * @return Long with the next seq id for the given sequence name
     */
    String sequencedIdPrimary(String seqName, Long staggerMax, Long bankSize);

    /** Gets the group name for specified entityName
     * @param entityName The name of the entity to get the group name
     * @return String with the group name that corresponds to the entityName
     */
    String getEntityGroupName(String entityName);

    /** Use this to get a Connection if you want to do JDBC operations directly. This Connection will be enlisted in
     * the active Transaction.
     *
     * @param groupName The name of entity group to get a connection for.
     *     Corresponds to the entity.group-name attribute and the moqui-conf datasource.group-name attribute.
     * @return JDBC Connection object for the associated database
     * @throws EntityException
     */
    Connection getConnection(String groupName) throws EntityException;

    // ======= XML Related Methods ========

    /** Make an object used to load XML entity data into the database or into an EntityList. The XML can come from
     * a specific location, XML text already read from somewhere, or by searching all component data directories
     * and the entity-facade.load-data elements for XML entity data files that match a type in the Set of types
     * specified.
     *
     * The document should have a root element like <code>&lt;entity-facade-xml type=&quot;seed&quot;&gt;</code>. The
     * type attribute will be used to determine if the file should be loaded by whether or not it matches the values
     * specified for data types on the loader.
     *
     * @return EntityDataLoader instance
     */
    EntityDataLoader makeDataLoader();

    /** Used to write XML entity data from the database to a writer.
     *
     * The document will have a root element like <code>&lt;entity-facade-xml&gt;</code>.
     *
     * @return EntityDataWriter instance
     */
    EntityDataWriter makeDataWriter();

    /** Make an EntityValue and populate it with the data (attributes and sub-elements) from the given XML element.
     *
     * @param element A XML DOM element representing a single value/record for an entity.
     * @return EntityValue object populated with data from the element.
     */
    EntityValue makeValue(Element element);
}
