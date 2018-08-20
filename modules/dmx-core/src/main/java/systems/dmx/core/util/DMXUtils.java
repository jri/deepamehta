package systems.dmx.core.util;

import systems.dmx.core.DMXObject;
import systems.dmx.core.Identifiable;
import systems.dmx.core.JSONEnabled;
import systems.dmx.core.Topic;
import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.RoleModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicRoleModel;
import systems.dmx.core.service.CoreService;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



public class DMXUtils {

    private static final Logger logger = Logger.getLogger(DMXUtils.class.getName());

    private static final String DM4_HOST_URL = System.getProperty("dmx.host.url");  // ### TODO: default value (#734)
    static {
        logger.info("Host setting:\ndmx.host.url=\"" + DM4_HOST_URL + "\"");
    }



    // ************
    // *** URLs ***
    // ************



    /**
     * Checks if an URL refers to this DMX installation.
     * The check relies on the "dmx.host.url" system property.
     */
    public static boolean isDMXURL(URL url) {
        try {
            return url.toString().startsWith(DM4_HOST_URL);
        } catch (Exception e) {
            throw new RuntimeException("Checking for DMX URL failed (url=\"" + url + "\")", e);
        }
    }



    // *******************
    // *** Collections ***
    // *******************



    public static List<Long> idList(Iterable<? extends Identifiable> items) {
        List<Long> ids = new ArrayList();
        for (Identifiable item : items) {
            ids.add(item.getId());
        }
        return ids;
    }

    public static <M> List<M> toModelList(Iterable<? extends DMXObject> objects) {
        List<M> modelList = new ArrayList();
        for (DMXObject object : objects) {
            modelList.add((M) object.getModel());
        }
        return modelList;
    }

    public static String topicNames(Iterable<? extends Topic> topics) {
        StringBuilder names = new StringBuilder();
        Iterator<? extends Topic> i = topics.iterator();
        while (i.hasNext()) {
            Topic topic = i.next();
            names.append('"').append(topic.getSimpleValue()).append('"');
            if (i.hasNext()) {
                names.append(", ");
            }
        }
        return names.toString();
    }

    public static <T extends DMXObject> List<T> loadChildTopics(List<T> objects) {
        for (DMXObject object : objects) {
            object.loadChildTopics();
        }
        return objects;
    }



    // ************
    // *** JSON ***
    // ************



    // === Generic ===

    public static Map toMap(JSONObject o) {
        return toMap(o, new HashMap());
    }

    public static Map toMap(JSONObject o, Map map) {
        try {
            Iterator<String> i = o.keys();
            while (i.hasNext()) {
                String key = i.next();
                map.put(key, o.get(key));   // throws JSONException
            }
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Converting JSONObject to Map failed", e);
        }
    }

    // ---

    public static List toList(JSONArray o) {
        try {
            List list = new ArrayList();
            for (int i = 0; i < o.length(); i++) {
                list.add(o.get(i));         // throws JSONException
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("Converting JSONArray to List failed", e);
        }
    }

    // === DMX specific ===

    public static JSONArray toJSONArray(Iterable<? extends JSONEnabled> items) {
        JSONArray array = new JSONArray();
        for (JSONEnabled item : items) {
            array.put(item.toJSON());
        }
        return array;
    }



    // *******************************
    // *** Association Auto-Typing ***
    // *******************************



    /**
     * Retypes the given association if its players match the given topic types. The given assoc model is modified
     * in-place. Typically called from a plugin's {@link systems.dmx.core.service.event.PreCreateAssociationListener}.
     *
     * <p>Read the parameters as follows: if "assoc" connects a "topicTypeUri1" with a "topicTypeUri2" (regardless of
     * 1,2 position) then retype it to "assocTypeUri" and use the role types "roleTypeUri1" and "roleTypeUri2".
     * "roleTypeUri1" is used for the "topicTypeUri1" player, "roleTypeUri2" is used for the "topicTypeUri2" player.
     *
     * <p>Auto-typing is supported only for topic players, and only if they are identified by-ID. If the given assoc has
     * at least one assoc player, or if a topic player is identfied by-URI, no retyping takes place (null is returned).
     *
     * @return  a 2-element {@link systems.dmx.core.model.RoleModel} array if auto-typing took place, <code>null</code>
     *          otherwise. Convenience to access the assoc's roles after retyping. Element 0 is the role of the
     *          "topicTypeUri1" player, Element 1 is the role of the "topicTypeUri2" player.
     */
    public static RoleModel[] associationAutoTyping(AssociationModel assoc, String topicTypeUri1, String topicTypeUri2,
                                                    String assocTypeUri, String roleTypeUri1, String roleTypeUri2,
                                                    CoreService dmx) {
        if (!assoc.getTypeUri().equals("dmx.core.association")) {
            return null;
        }
        RoleModel[] roles = getRoleModels(assoc, topicTypeUri1, topicTypeUri2, dmx);
        if (roles != null) {
            logger.info("### Auto typing association into \"" + assocTypeUri +
                "\" (\"" + topicTypeUri1 + "\" <-> \"" + topicTypeUri2 + "\")");
            assoc.setTypeUri(assocTypeUri);
            roles[0].setRoleTypeUri(roleTypeUri1);
            roles[1].setRoleTypeUri(roleTypeUri2);
        }
        return roles;
    }

    public static RoleModel[] getRoleModels(AssociationModel assoc, String topicTypeUri1, String topicTypeUri2,
                                                                                          CoreService dmx) {
        RoleModel r1 = assoc.getRoleModel1();
        RoleModel r2 = assoc.getRoleModel2();
        // ### FIXME: auto-typing is supported only for topic players, and if they are identified by-ID.
        // Note: we can't call roleModel.getPlayer() as this would build an entire object model, but its "value"
        // is not yet available in case the association is part of the player's composite structure.
        // Compare to AssociationModelImpl.duplicateCheck()
        if (!(r1 instanceof TopicRoleModel) || ((TopicRoleModel) r1).topicIdentifiedByUri() ||
            !(r2 instanceof TopicRoleModel) || ((TopicRoleModel) r2).topicIdentifiedByUri()) {
            return null;
        }
        String t1 = (String) dmx.getProperty(r1.getPlayerId(), "typeUri");
        String t2 = (String) dmx.getProperty(r2.getPlayerId(), "typeUri");
        RoleModel roleModel1 = getRoleModel(r1, r2, t1, t2, topicTypeUri1, 1);
        RoleModel roleModel2 = getRoleModel(r1, r2, t1, t2, topicTypeUri2, 2);
        if (roleModel1 != null && roleModel2 != null) {
            return new RoleModel[] {roleModel1, roleModel2};
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private static RoleModel getRoleModel(RoleModel r1, RoleModel r2, String t1, String t2, String topicTypeUri,
                                                                                            int nr) {
        boolean m1 = t1.equals(topicTypeUri);
        boolean m2 = t2.equals(topicTypeUri);
        if (m1 && m2) {
            return nr == 1 ? r1 : r2;
        }
        return m1 ? r1 : m2 ? r2 : null;
    }
}
