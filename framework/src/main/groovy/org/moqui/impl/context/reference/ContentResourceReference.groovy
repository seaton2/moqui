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
package org.moqui.impl.context.reference

import javax.jcr.Session
import javax.jcr.Property

import org.moqui.context.ExecutionContext
import org.moqui.context.ResourceReference
import org.moqui.impl.StupidUtilities
import org.moqui.impl.context.ResourceFacadeImpl

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ContentResourceReference extends BaseResourceReference {
    protected final static Logger logger = LoggerFactory.getLogger(ContentResourceReference.class)
    public final static String locationPrefix = "content://"

    String location
    String repositoryName
    String nodePath

    protected javax.jcr.Node theNode = null

    ContentResourceReference() { }
    
    @Override
    ResourceReference init(String location, ExecutionContext ec) {
        this.ec = ec

        this.location = location
        // TODO: change to not rely on URI, or to encode properly
        URI locationUri = new URI(location)
        repositoryName = locationUri.host
        nodePath = locationUri.path

        return this
    }

    ResourceReference init(String repositoryName, javax.jcr.Node node, ExecutionContext ec) {
        this.repositoryName = repositoryName
        this.nodePath = node.path
        this.location = "${locationPrefix}${repositoryName}/${nodePath}"
        this.theNode = node
        return this
    }

    @Override
    String getLocation() { location }

    @Override
    InputStream openStream() {
        javax.jcr.Node node = getNode()
        if (node == null) return null
        javax.jcr.Node contentNode = node.getNode("jcr:content")
        if (contentNode == null) throw new IllegalArgumentException("Cannot get stream for content at [${repositoryName}][${nodePath}], has no jcr:content child node")
        Property dataProperty = contentNode.getProperty("jcr:data")
        if (dataProperty == null) throw new IllegalArgumentException("Cannot get stream for content at [${repositoryName}][${nodePath}], has no jcr:content.jcr:data property")
        return dataProperty.binary.stream
    }

    @Override
    String getText() { return StupidUtilities.getStreamText(openStream()) }

    @Override
    boolean supportsAll() { true }

    @Override
    boolean supportsUrl() { false }
    @Override
    URL getUrl() { return null }

    @Override
    boolean supportsDirectory() { true }
    @Override
    boolean isFile() {
        javax.jcr.Node node = getNode()
        if (node == null) return false
        return node.isNodeType("nt:file")
    }
    @Override
    boolean isDirectory() {
        javax.jcr.Node node = getNode()
        if (node == null) return false
        return node.isNodeType("nt:folder")
    }
    @Override
    List<ResourceReference> getDirectoryEntries() {
        List<ResourceReference> dirEntries = new LinkedList()
        javax.jcr.Node node = getNode()
        if (node == null) return dirEntries

        for (javax.jcr.Node childNode in node.getNodes()) {
            dirEntries.add(new ContentResourceReference().init(repositoryName, childNode, ec))
        }
        return dirEntries
    }
    // TODO: consider overriding findChildFile() to let the JCR impl do the query
    // ResourceReference findChildFile(String relativePath)

    @Override
    boolean supportsExists() { true }
    @Override
    boolean getExists() {
        if (theNode != null) return true
        Session session = ((ResourceFacadeImpl) ec.resource).getContentRepositorySession(repositoryName)
        return session.nodeExists(nodePath)
    }

    boolean supportsLastModified() { false }
    long getLastModified() {
        // TODO: more research to see if we can get a last modified time
        System.currentTimeMillis()
    }

    boolean supportsWrite() { true }

    void putText(String text) {
        putObject(text)
    }
    void putStream(InputStream stream) {
        putObject(stream)
    }
    protected void putObject(Object obj) {
        if (obj == null) {
            logger.warn("Data was null, not saving to resource [${getLocation()}]")
            return
        }
        Session session = ((ResourceFacadeImpl) ec.resource).getContentRepositorySession(repositoryName)
        javax.jcr.Node fileNode = getNode()
        if (fileNode != null) {
            javax.jcr.Node fileContent = fileNode.getNode("jcr:content")
            fileContent.setProperty("jcr:data", session.valueFactory.createValue(text))
            session.save()
        } else {
            // first make sure the directory exists that this is in
            List<String> nodePathList = nodePath.split('/')
            // if nodePath started with a '/' the first element will be empty
            if (nodePathList && nodePathList[0] == "") nodePathList.remove(0)
            // remove the filename to just get the directory
            if (nodePathList) nodePathList.remove(nodePathList.size()-1)
            javax.jcr.Node folderNode = findDirectoryNode(session, nodePathList, true)

            // now write the text to the node and save it
            fileNode = folderNode.addNode(fileName, "nt:file")
            javax.jcr.Node fileContent = fileNode.addNode("jcr:content", "nt:resource")
            fileContent.setProperty("jcr:mimeType", contentType)
            // fileContent.setProperty("jcr:encoding", ?)
            Calendar lastModified = Calendar.getInstance(); lastModified.setTimeInMillis(System.currentTimeMillis())
            fileContent.setProperty("jcr:lastModified", lastModified)
            if (obj instanceof String) {
                fileContent.setProperty("jcr:data", session.valueFactory.createValue((String) obj))
            } else if (obj instanceof InputStream) {
                fileContent.setProperty("jcr:data", session.valueFactory.createBinary((InputStream) obj))
            } else {
                throw new IllegalArgumentException("Cannot save content for obj with type ${obj.class.name}")
            }

            session.save()
        }
    }

    static javax.jcr.Node findDirectoryNode(Session session, List<String> pathList, boolean create) {
        javax.jcr.Node rootNode = session.getRootNode()
        javax.jcr.Node folderNode = rootNode
        if (pathList) {
            for (String nodePathElement in pathList) {
                if (folderNode.hasNode(nodePathElement)) {
                    folderNode = folderNode.getNode(nodePathElement)
                } else {
                    if (create) {
                        folderNode = folderNode.addNode(nodePathElement, "nt:folder")
                    } else {
                        folderNode = null
                        break
                    }
                }
            }
        }
        return folderNode
    }

    void move(String newLocation) {
        if (!newLocation.startsWith(locationPrefix))
            throw new IllegalArgumentException("New location [${newLocation}] is not a content location, not moving resource at ${getLocation()}")

        Session session = ((ResourceFacadeImpl) ec.resource).getContentRepositorySession(repositoryName)

        ResourceReference newRr = ec.resource.getLocationReference(newLocation)
        if (!newRr instanceof ContentResourceReference)
            throw new IllegalArgumentException("New location [${newLocation}] is not a content location, not moving resource at ${getLocation()}")
        ContentResourceReference newCrr = (ContentResourceReference) newRr

        // make sure the target folder exists
        List<String> nodePathList = newCrr.getNodePath().split('/')
        if (nodePathList) nodePathList.remove(nodePathList.size()-1)
        findDirectoryNode(session, nodePathList, true)

        session.move(this.getNodePath(), newCrr.getNodePath())

        this.theNode = null
    }

    javax.jcr.Node getNode() {
        if (theNode != null) return theNode
        Session session = ((ResourceFacadeImpl) ec.resource).getContentRepositorySession(repositoryName)
        return session.nodeExists(nodePath) ? session.getNode(nodePath) : null
    }
}
