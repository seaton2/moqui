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
package org.moqui.service;

import java.util.Map;

public interface ServiceCallSync extends ServiceCall {
    /** Name of the service to run. The combined service name, like: "${path}.${verb}${noun}". To explicitly separate
     * the verb and noun put a hash (#) between them, like: "${path}.${verb}#${noun}" (this is useful for calling the
     * implicit entity CrUD services where verb is create, update, or delete and noun is the name of the entity).
     */
    ServiceCallSync name(String serviceName);

    ServiceCallSync name(String verb, String noun);

    ServiceCallSync name(String path, String verb, String noun);

    /** Map of name, value pairs that make up the context (in parameters) passed to the service. */
    ServiceCallSync parameters(Map<String, ?> context);

    /** Single name, value pairs to put in the context (in parameters) passed to the service. */
    ServiceCallSync parameter(String name, Object value);


    /** If true suspend and create a new transaction if a transaction is active.
     * @return Reference to this for convenience.
     */
    ServiceCallSync requireNewTransaction(boolean requireNewTransaction);

    /** If true expect multiple sets of parameters passed in a single map, each set with a suffix of an underscore
     * and the row of the number, ie something like "userId_8" for the 8th row.
     * @return Reference to this for convenience.
     */
    ServiceCallSync multi(boolean mlt);

    /* * If null defaults to configured value for service, or container. For possible values see JavaDoc for javax.sql.Connection.
     * @return Reference to this for convenience.
     */
    /* not supported by Atomikos/etc right now, consider for later: ServiceCallSync transactionIsolation(int transactionIsolation);

    /** Call the service synchronously and immediately get the result.
     * @return Map containing the result (out parameters) from the service call.
     */
    Map<String, Object> call() throws ServiceException;
}
