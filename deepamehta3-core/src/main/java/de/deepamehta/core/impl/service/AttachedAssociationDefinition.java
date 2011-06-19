package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.ViewConfigurationModel;

import org.codehaus.jettison.json.JSONObject;

import java.util.Set;
import java.util.logging.Logger;



/**
 * An association definition that is attached to the {@link DeepaMehtaService}.
 */
class AttachedAssociationDefinition extends AttachedAssociation implements AssociationDefinition {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AttachedViewConfiguration viewConfig;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedAssociationDefinition(EmbeddedService dms) {
        super(dms);     // The model and viewConfig remain uninitialized.
                        // They are initialized later on through fetch().
    }

    AttachedAssociationDefinition(AssociationDefinitionModel model, EmbeddedService dms) {
        super(model, dms);
        initViewConfig();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === AssociationDefinition Implementation ===

    @Override
    public String getInstanceLevelAssocTypeUri() {
        return getModel().getInstanceLevelAssocTypeUri();
    }

    @Override
    public String getWholeTopicTypeUri() {
        return getModel().getWholeTopicTypeUri();
    }

    @Override
    public String getPartTopicTypeUri() {
        return getModel().getPartTopicTypeUri();
    }

    @Override
    public String getWholeRoleTypeUri() {
        return getModel().getWholeRoleTypeUri();
    }

    @Override
    public String getPartRoleTypeUri() {
        return getModel().getPartRoleTypeUri();
    }

    @Override
    public String getWholeCardinalityUri() {
        return getModel().getWholeCardinalityUri();
    }

    @Override
    public String getPartCardinalityUri() {
        return getModel().getPartCardinalityUri();
    }

    @Override
    public ViewConfiguration getViewConfig() {
        return viewConfig;
    }

    // ---

    @Override
    public void setWholeCardinalityUri(String wholeCardinalityUri) {
        getModel().setWholeCardinalityUri(wholeCardinalityUri);
    }

    @Override
    public void setPartCardinalityUri(String partCardinalityUri) {
        getModel().setPartCardinalityUri(partCardinalityUri);
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    /**
     * @param   topicTypeUri    only used for sanity check
     */
    void fetch(Association assoc, String topicTypeUri) {
        try {
            TopicTypes topicTypes = fetchTopicTypes(assoc);
            // ### RoleTypes roleTypes = fetchRoleTypes(assoc);
            Cardinality cardinality = fetchCardinality(assoc);
            // sanity check
            if (!topicTypes.wholeTopicTypeUri.equals(topicTypeUri)) {
                throw new RuntimeException("jri doesn't understand Neo4j traversal");
            }
            //
            AssociationDefinitionModel model = new AssociationDefinitionModel(assoc.getId(), assoc.getTypeUri(),
                topicTypes.wholeTopicTypeUri, topicTypes.partTopicTypeUri
                /* ###, roleTypes.wholeRoleTypeUri, roleTypes.partRoleTypeUri */);
            model.setWholeCardinalityUri(cardinality.wholeCardinalityUri);
            model.setPartCardinalityUri(cardinality.partCardinalityUri);
            model.setViewConfigModel(fetchViewConfig(assoc));
            //
            setModel(model);
            initViewConfig();
        } catch (Exception e) {
            throw new RuntimeException("Fetching association definition for topic type \"" + topicTypeUri +
                "\" failed (" + assoc + ")", e);
        }
    }

    /**
     * @param   predecessor     The predecessor of the new assocdef. The new assocdef is added after this one.
     *                          <code>null</code> indicates the sequence start. 
     */
    void store(AssociationDefinition predecessor) {
        try {
            // Note: creating the underlying association is conditional. It exists already for
            // an interactively created association definition. Its ID is already set.
            if (getId() == -1) {
                dms.createAssociation(getModel(), null);    // clientContext=null
            }
            // role types
            dms.createAssociation("dm3.core.aggregation",
                new TopicRoleModel(getWholeRoleTypeUri(), "dm3.core.whole_role_type"),
                new AssociationRoleModel(getId(), "dm3.core.assoc_def"));
            dms.createAssociation("dm3.core.aggregation",
                new TopicRoleModel(getPartRoleTypeUri(), "dm3.core.part_role_type"),
                new AssociationRoleModel(getId(), "dm3.core.assoc_def"));
            // cardinality
            dms.createAssociation("dm3.core.aggregation",
                new TopicRoleModel(getWholeCardinalityUri(), "dm3.core.whole_cardinality"),
                new AssociationRoleModel(getId(), "dm3.core.assoc_def"));
            dms.createAssociation("dm3.core.aggregation",
                new TopicRoleModel(getPartCardinalityUri(), "dm3.core.part_cardinality"),
                new AssociationRoleModel(getId(), "dm3.core.assoc_def"));
            //
            putInSequence(predecessor);
            //
            storeViewConfig();
        } catch (Exception e) {
            throw new RuntimeException("Storing association definition \"" + getUri() +
                "\" of topic type \"" + getWholeTopicTypeUri() + "\" failed", e);
        }
    }

    // ---

    @Override
    public AssociationDefinitionModel getModel() {
        return (AssociationDefinitionModel) super.getModel();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Fetch ===

    // Note: in the fetch methods the assoc def's model isn't available. It doesn't exist yet.
    // The model is only created by these very fetch methods.

    private TopicTypes fetchTopicTypes(Association assoc) {
        String wholeTopicTypeUri = getWholeTopicTypeUri(assoc);
        String partTopicTypeUri = getPartTopicTypeUri(assoc);
        return new TopicTypes(wholeTopicTypeUri, partTopicTypeUri);
    }

    /* ### private RoleTypes fetchRoleTypes(Association assoc) {
        Topic wholeRoleType = assoc.getTopic("dm3.core.whole_role_type");
        Topic partRoleType = assoc.getTopic("dm3.core.part_role_type");
        RoleTypes roleTypes = new RoleTypes();
        // role types are optional
        if (wholeRoleType != null) {
            roleTypes.setWholeRoleTypeUri(wholeRoleType.getUri());
        }
        if (partRoleType != null) {
            roleTypes.setPartRoleTypeUri(partRoleType.getUri());
        }
        return roleTypes;
    } */

    private Cardinality fetchCardinality(Association assoc) {
        Topic wholeCardinality = assoc.getRelatedTopic("dm3.core.aggregation", "dm3.core.assoc_def",
            "dm3.core.whole_cardinality", "dm3.core.cardinality", false);    // fetchComposite=false
        Topic partCardinality = assoc.getRelatedTopic("dm3.core.aggregation", "dm3.core.assoc_def",
            "dm3.core.part_cardinality", "dm3.core.cardinality", false);    // fetchComposite=false
        Cardinality cardinality = new Cardinality();
        if (wholeCardinality != null) {
            cardinality.setWholeCardinalityUri(wholeCardinality.getUri());
        }
        if (partCardinality != null) {
            cardinality.setPartCardinalityUri(partCardinality.getUri());
        } else {
            throw new RuntimeException("Missing cardinality of position 2");
        }
        return cardinality;
    }

    private ViewConfigurationModel fetchViewConfig(Association assoc) {
        // ### should we use "dm3.core.association" instead of "dm3.core.aggregation"?
        Set<RelatedTopic> topics = assoc.getRelatedTopics("dm3.core.aggregation", "dm3.core.assoc_def",
            "dm3.core.view_config", null, true);    // fetchComposite=true
        // Note: the view config's topic type is unknown (it is client-specific), othersTopicTypeUri=null
        return new ViewConfigurationModel(dms.getTopicModels(topics));
    }

    // --- Inner Classes ---

    private class TopicTypes {

        private String wholeTopicTypeUri;
        private String partTopicTypeUri;

        private TopicTypes(String wholeTopicTypeUri, String partTopicTypeUri) {
            this.wholeTopicTypeUri = wholeTopicTypeUri;
            this.partTopicTypeUri = partTopicTypeUri;
        }
    }

    /* ### private class RoleTypes {

        private String wholeRoleTypeUri;
        private String partRoleTypeUri;

        private void setWholeRoleTypeUri(String wholeRoleTypeUri) {
            this.wholeRoleTypeUri = wholeRoleTypeUri;
        }

        private void setPartRoleTypeUri(String partRoleTypeUri) {
            this.partRoleTypeUri = partRoleTypeUri;
        }
    } */

    private class Cardinality {

        private String wholeCardinalityUri;
        private String partCardinalityUri;

        private void setWholeCardinalityUri(String wholeCardinalityUri) {
            this.wholeCardinalityUri = wholeCardinalityUri;
        }

        private void setPartCardinalityUri(String partCardinalityUri) {
            this.partCardinalityUri = partCardinalityUri;
        }
    }



    // === Store ===

    private void putInSequence(AssociationDefinition predecessor) {
        if (predecessor == null) {
            // start sequence
            AssociationModel assocModel = new AssociationModel("dm3.core.association");
            assocModel.setRoleModel1(new TopicRoleModel(getWholeTopicTypeUri(), "dm3.core.topic_type"));
            assocModel.setRoleModel2(new AssociationRoleModel(getId(), "dm3.core.first_assoc_def"));
            dms.createAssociation(assocModel, null);            // FIXME: clientContext=null
        } else {
            // continue sequence
            AssociationModel assocModel = new AssociationModel("dm3.core.sequence");
            assocModel.setRoleModel1(new AssociationRoleModel(predecessor.getId(), "dm3.core.predecessor"));
            assocModel.setRoleModel2(new AssociationRoleModel(getId(), "dm3.core.successor"));
            dms.createAssociation(assocModel, null);            // FIXME: clientContext=null
        }
    }

    private void storeViewConfig() {
        for (TopicModel configTopic : getModel().getViewConfigModel().getConfigTopics()) {
            Topic topic = dms.createTopic(configTopic, null);   // FIXME: clientContext=null
            dms.createAssociation("dm3.core.aggregation",
                new AssociationRoleModel(getId(), "dm3.core.assoc_def"),
                new TopicRoleModel(topic.getId(), "dm3.core.view_config"));
        }
    }



    // === Helper ===

    private void initViewConfig() {
        RoleModel configurable = new AssociationRoleModel(getId(), "dm3.core.assoc_def");
        this.viewConfig = new AttachedViewConfiguration(configurable, getModel().getViewConfigModel(), dms);
    }

    // ---

    // ### FIXME: copy in TypeEditorPlugin
    private String getWholeTopicTypeUri(Association assoc) {
        return assoc.getTopic("dm3.core.whole_topic_type").getUri();
    }

    // ### FIXME: copy in TypeEditorPlugin
    private String getPartTopicTypeUri(Association assoc) {
        return assoc.getTopic("dm3.core.part_topic_type").getUri();
    }
}
