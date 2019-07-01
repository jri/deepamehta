package systems.dmx.core.storage.spi;

import systems.dmx.core.impl.AssocModelImpl;
import systems.dmx.core.impl.TopicModelImpl;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.DMXObjectModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.RelatedAssocModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.ModelFactory;

import java.util.List;



public interface DMXStorage {



    // === Topics ===

    /**
     * @return  The fetched topic.
     *          Note: its child topics are not fetched.
     */
    TopicModelImpl fetchTopic(long topicId);

    /**
     * Looks up a single topic by exact value.
     *
     * @return  The fetched topic, or <code>null</code> if no such topic exists.
     *          Note: its child topics are not fetched.
     *
     * @throws  RuntimeException    if more than one topic is found.
     */
    TopicModelImpl fetchTopic(String key, Object value);

    List<TopicModelImpl> fetchTopics(String key, Object value);

    List<TopicModelImpl> queryTopics(Object value);

    /**
     * @return  The fetched topics.
     *          Note: their child topics are not fetched.
     */
    List<TopicModelImpl> queryTopics(String key, Object value);

    Iterable<TopicModelImpl> fetchAllTopics();

    // ---

    /**
     * Stores a rudimentary topic in the DB.
     * <p>
     * Only the topic URI and the topic type URI are stored.
     * The topic value (simple or composite) is <i>not</i> stored.
     * The "Instantiation" association is <i>not</i> stored.
     * <p>
     * An URI uniqueness check is performed. If the DB already contains a topic or an association with
     * the URI passed, an exception is thrown and nothing is stored.
     *
     * @param   topicModel  The topic to store. Once the method returns the topic model contains:
     *                          - the ID of the stored topic.
     *                          - an empty URI ("") in case <code>null</code> was passed.
     *                          - an empty simple value ("") in case <code>null</code> was passed.
     */
    void storeTopic(TopicModel topicModel);

    /**
     * Stores and indexes the topic's URI.
     */
    void storeTopicUri(long topicId, String uri);

    void storeTopicTypeUri(long topicId, String topicTypeUri);

    /**
     * Stores and indexes a topic value.
     *
     * @param   indexKey    must not null
     * @param   indexValue  Optional: the value to be indexed. If indexValue is not specified, value is used. ### FIXDOC
     */
    void storeTopicValue(long topicId, SimpleValue value, String indexKey, boolean isHtmlValue);

    // ---

    /**
     * Deletes the topic.
     * <p>
     * Prerequisite: the topic has no relations.
     */
    void deleteTopic(long topicId);



    // === Associations ===

    AssocModelImpl fetchAssoc(long assocId);

    /**
     * Looks up a single association by exact value.
     * If no such association exists <code>null</code> is returned.
     * If more than one association is found a runtime exception is thrown.
     *
     * @return  The fetched association.
     *          Note: its child topics are not fetched.
     */
    AssocModelImpl fetchAssoc(String key, Object value);

    List<AssocModelImpl> fetchAssocs(String key, Object value);

    /**
     * Returns the associations between two topics. If no such association exists an empty set is returned.
     *
     * @param   assocTypeUri    Assoc type filter. Pass <code>null</code> to switch filter off.
     */
    List<AssocModelImpl> fetchAssocs(String assocTypeUri, long topicId1, long topicId2, String roleTypeUri1,
                                                                                        String roleTypeUri2);

    List<? extends AssocModel> fetchAssocsBetweenTopicAndAssoc(String assocTypeUri, long topicId, long assocId,
                                                               String topicRoleTypeUri, String assocRoleTypeUri);

    Iterable<? extends AssocModel> fetchAllAssocs();

    List<PlayerModel> fetchPlayerModels(long assocId);

    // ---

    void storeAssoc(AssocModel assocModel);

    void storeAssocUri(long assocId, String uri);

    void storeAssocTypeUri(long assocId, String assocTypeUri);

    /**
     * Stores and indexes an association value.
     *
     * @param   indexKey    must not null
     * @param   indexValue  Optional: the value to be indexed. If indexValue is not specified, value is used. ### FIXDOC
     */
    void storeAssocValue(long assocId, SimpleValue value, String indexKey, boolean isHtmlValue);

    void storeRoleTypeUri(long assocId, long playerId, String roleTypeUri);

    // ---

    void deleteAssoc(long assocId);



    // === Generic Object ===

    DMXObjectModel fetchObject(long id);



    // === Traversal ===

    List<? extends AssocModel> fetchTopicAssocs(long topicId);

    List<? extends AssocModel> fetchAssocAssocs(long assocId);

    // ---

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    List<? extends RelatedTopicModel> fetchTopicRelatedTopics(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                              String othersRoleTypeUri, String othersTopicTypeUri);

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    List<? extends RelatedAssocModel> fetchTopicRelatedAssocs(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                                   String othersRoleTypeUri, String othersAssocTypeUri);

    // ---

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    List<? extends RelatedTopicModel> fetchAssocRelatedTopics(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                              String othersRoleTypeUri, String othersTopicTypeUri);

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    List<? extends RelatedAssocModel> fetchAssocRelatedAssocs(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                              String othersRoleTypeUri, String othersAssocTypeUri);

    // ---

    /**
     * @param   objectId            id of a topic or an association
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    List<? extends RelatedTopicModel> fetchRelatedTopics(long objectId, String assocTypeUri, String myRoleTypeUri,
                                                         String othersRoleTypeUri, String othersTopicTypeUri);

    /**
     * @param   objectId            id of a topic or an association
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    List<? extends RelatedAssocModel> fetchRelatedAssocs(long objectId, String assocTypeUri, String myRoleTypeUri,
                                                         String othersRoleTypeUri, String othersAssocTypeUri);



    // === Properties ===

    /**
     * @param   id                  id of a topic or an association
     */
    Object fetchProperty(long id, String propUri);

    /**
     * @param   id                  id of a topic or an association
     */
    boolean hasProperty(long id, String propUri);

    // ---

    List<TopicModelImpl> fetchTopicsByProperty(String propUri, Object propValue);

    List<TopicModelImpl> fetchTopicsByPropertyRange(String propUri, Number from, Number to);

    List<AssocModelImpl> fetchAssocsByProperty(String propUri, Object propValue);

    List<AssocModelImpl> fetchAssocsByPropertyRange(String propUri, Number from, Number to);

    // ---

    void storeTopicProperty(long topicId, String propUri, Object propValue, boolean addToIndex);

    void storeAssocProperty(long assocId, String propUri, Object propValue, boolean addToIndex);

    // ---

    void indexTopicProperty(long topicId, String propUri, Object propValue);

    void indexAssocProperty(long assocId, String propUri, Object propValue);

    // ---

    void deleteTopicProperty(long topicId, String propUri);

    void deleteAssocProperty(long assocId, String propUri);



    // === DB ===

    DMXTransaction beginTx();

    boolean setupRootNode();

    void shutdown();

    // ---

    Object getDatabaseVendorObject();

    Object getDatabaseVendorObject(long objectId);

    // ---

    ModelFactory getModelFactory();
}
