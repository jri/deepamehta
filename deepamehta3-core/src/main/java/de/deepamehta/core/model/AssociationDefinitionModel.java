package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



/**
 * Definition of an association between 2 topic types -- part of DeepaMehta's type system,
 * like an association in a class diagram. Used to represent both, aggregations and compositions.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class AssociationDefinitionModel extends AssociationModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String instanceLevelAssocTypeUri;

    private String wholeTopicTypeUri;
    private String partTopicTypeUri;

    private String wholeRoleTypeUri;    // value might be derived, there is not necessarily such a role type topic
    private String partRoleTypeUri;     // value might be derived, there is not necessarily such a role type topic

    private String wholeCardinalityUri;
    private String partCardinalityUri;

    private ViewConfigurationModel viewConfigModel;   // is never null

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationDefinitionModel(long id, String typeUri, String wholeTopicTypeUri, String partTopicTypeUri
                                          /* ### String wholeRoleTypeUri, String partRoleTypeUri */) {
        this.id = id;
        this.typeUri = typeUri;
        //
        this.wholeTopicTypeUri = wholeTopicTypeUri;
        this.partTopicTypeUri = partTopicTypeUri;
        // set default role types
        this.wholeRoleTypeUri = "dm3.core.whole";// ### wholeRoleTypeUri != null ? wholeRoleTypeUri : wholeTopicTypeUri;
        this.partRoleTypeUri = "dm3.core.part";  // ### partRoleTypeUri != null ? partRoleTypeUri : partTopicTypeUri;
        // derive uri
        this.uri = partTopicTypeUri;             // ### partRoleTypeUri;
        //
        initAssociationModel();
        initInstanceLevelAssocTypeUri();
    }

    public AssociationDefinitionModel(JSONObject assocDef, String wholeTopicTypeUri) {
        try {
            this.id = -1;
            this.typeUri = assocDef.getString("assoc_type_uri");
            //
            this.wholeTopicTypeUri = wholeTopicTypeUri;
            this.partTopicTypeUri = assocDef.getString("part_topic_type_uri");
            //
            this.wholeRoleTypeUri = "dm3.core.whole";// ## assocDef.optString("whole_role_type_uri", wholeTopicTypeUri);
            this.partRoleTypeUri = "dm3.core.part";  // ## assocDef.optString("part_role_type_uri", partTopicTypeUri);
            //
            this.uri = partTopicTypeUri;             // ### partRoleTypeUri;
            //
            if (!assocDef.has("whole_cardinality_uri") && !typeUri.equals("dm3.core.composition_def")) {
                throw new RuntimeException("\"whole_cardinality_uri\" is missing");
            }
            this.wholeCardinalityUri = assocDef.optString("whole_cardinality_uri", "dm3.core.one");
            this.partCardinalityUri = assocDef.getString("part_cardinality_uri");
            //
            this.viewConfigModel = new ViewConfigurationModel(assocDef);
            //
            initAssociationModel();
            initInstanceLevelAssocTypeUri();
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationDefinitionModel failed (JSONObject=" + assocDef + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getInstanceLevelAssocTypeUri() {
        return instanceLevelAssocTypeUri;
    }

    public String getWholeTopicTypeUri() {
        return wholeTopicTypeUri;
    }

    public String getPartTopicTypeUri() {
        return partTopicTypeUri;
    }

    public String getWholeRoleTypeUri() {
        return wholeRoleTypeUri;
    }

    public String getPartRoleTypeUri() {
        return partRoleTypeUri;
    }

    public String getWholeCardinalityUri() {
        return wholeCardinalityUri;
    }

    public String getPartCardinalityUri() {
        return partCardinalityUri;
    }

    public ViewConfigurationModel getViewConfigModel() {
        return viewConfigModel;
    }

    // ---

    @Override
    public void setTypeUri(String typeUri) {
        super.setTypeUri(typeUri);
        initInstanceLevelAssocTypeUri();
    }

    public void setWholeCardinalityUri(String wholeCardinalityUri) {
        this.wholeCardinalityUri = wholeCardinalityUri;
    }

    public void setPartCardinalityUri(String partCardinalityUri) {
        this.partCardinalityUri = partCardinalityUri;
    }

    public void setViewConfigModel(ViewConfigurationModel viewConfigModel) {
        this.viewConfigModel = viewConfigModel;
    }

    // ---

    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("uri", uri);
            o.put("assoc_type_uri", typeUri);
            o.put("whole_topic_type_uri", wholeTopicTypeUri);
            o.put("part_topic_type_uri", partTopicTypeUri);
            o.put("whole_role_type_uri", wholeRoleTypeUri);
            o.put("part_role_type_uri", partRoleTypeUri);
            o.put("whole_cardinality_uri", wholeCardinalityUri);
            o.put("part_cardinality_uri", partCardinalityUri);
            viewConfigModel.toJSON(o);
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // ---

    @Override
    public String toString() {
        return "\n    association definition (id=" + id + ", uri=\"" + uri + "\", typeUri=\"" + typeUri +
            "\")\n        pos 1: (type=\"" + wholeTopicTypeUri + "\", role=\"" + wholeRoleTypeUri +
            "\", cardinality=\"" + wholeCardinalityUri +
            "\")\n        pos 2: (type=\"" + partTopicTypeUri + "\", role=\"" + partRoleTypeUri +
            "\", cardinality=\"" + partCardinalityUri +
            "\")\n        association definition " + viewConfigModel;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    static void toJSON(Collection<AssociationDefinitionModel> assocDefs, JSONObject o) throws Exception {
        List assocDefList = new ArrayList();
        for (AssociationDefinitionModel assocDef : assocDefs) {
            assocDefList.add(assocDef.toJSON());
        }
        o.put("assoc_defs", assocDefList);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void initAssociationModel() {
        setRoleModel1(new TopicRoleModel(wholeTopicTypeUri, "dm3.core.whole_topic_type"));
        setRoleModel2(new TopicRoleModel(partTopicTypeUri,  "dm3.core.part_topic_type"));
    }

    private void initInstanceLevelAssocTypeUri() {
        if (typeUri.equals("dm3.core.aggregation_def")) {
            this.instanceLevelAssocTypeUri = "dm3.core.aggregation";
        } else if (typeUri.equals("dm3.core.composition_def")) {
            this.instanceLevelAssocTypeUri = "dm3.core.composition";
        } else {
            throw new RuntimeException("Unexpected association type URI: \"" + typeUri + "\"");
        }
    }
}
