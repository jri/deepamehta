package systems.dmx.core.impl;

import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.facets.FacetValueModel;
import systems.dmx.core.service.ModelFactory;

import java.util.HashMap;
import java.util.List;



class FacetValueModelImpl extends ChildTopicsModelImpl implements FacetValueModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String childTypeUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    FacetValueModelImpl(String childTypeUri, ModelFactory mf) {
        super(new HashMap(), mf);
        this.childTypeUri = childTypeUri;
    }

    FacetValueModelImpl(ChildTopicsModelImpl childTopics) {
        super(childTopics);
        this.childTypeUri = iterator().next();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Single-valued Facets ===

    public RelatedTopicModel getTopic() {
        return getTopic(childTypeUri);
    }

    public List<? extends RelatedTopicModel> getTopics() {
        return getTopics(childTypeUri);
    }

    // --- ### TODO: rename to "set" (4x)

    public FacetValueModel put(RelatedTopicModel value) {
        return (FacetValueModel) set(childTypeUri, value);
    }

    public FacetValueModel put(TopicModel value) {
        return (FacetValueModel) set(childTypeUri, value);
    }

    // ---

    public FacetValueModel put(Object value) {
        return (FacetValueModel) set(childTypeUri, value);
    }

    public FacetValueModel put(ChildTopicsModel value) {
        return (FacetValueModel) set(childTypeUri, value);
    }

    // --- ### TODO: rename to "setRef" (2x)

    public FacetValueModel putRef(long refTopicId) {
        return (FacetValueModel) setRef(childTypeUri, refTopicId);
    }

    public FacetValueModel putRef(String refTopicUri) {
        return (FacetValueModel) setRef(childTypeUri, refTopicUri);
    }

    // --- ### TODO: rename to "setDeletionRef" (2x)

    public FacetValueModel putDeletionRef(long refTopicId) {
        return (FacetValueModel) setDeletionRef(childTypeUri, refTopicId);
    }

    public FacetValueModel putDeletionRef(String refTopicUri) {
        return (FacetValueModel) setDeletionRef(childTypeUri, refTopicUri);
    }



    // === Multiple-valued Facets ===

    // --- ### TODO: rename to "set"
    public FacetValueModel put(List<RelatedTopicModel> values) {
        return (FacetValueModel) set(childTypeUri, values);
    }

    // ---

    public FacetValueModel addRef(long refTopicId) {
        return (FacetValueModel) addRef(childTypeUri, refTopicId);
    }

    public FacetValueModel addRef(String refTopicUri) {
        return (FacetValueModel) addRef(childTypeUri, refTopicUri);
    }

    // ---

    public FacetValueModel addDeletionRef(long refTopicId) {
        return (FacetValueModel) addDeletionRef(childTypeUri, refTopicId);
    }

    public FacetValueModel addDeletionRef(String refTopicUri) {
        return (FacetValueModel) addDeletionRef(childTypeUri, refTopicUri);
    }
}
