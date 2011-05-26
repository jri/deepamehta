function Canvas() {

    // ------------------------------------------------------------------------------------------------ Constructor Code

    this.superclass = TopicmapRenderer
    this.superclass()
    var self = this

    // Settings
    var ACTIVE_COLOR = "red"
    var ACTIVE_TOPIC_WIDTH = 3
    var ACTIVE_ASSOC_WIDTH = 10
    var ASSOC_COLOR = "#b0b0b0"
    var ASSOC_WIDTH = 4
    var ASSOC_CLICK_TOLERANCE = 0.3
    var CANVAS_ANIMATION_STEPS = 30
    var HIGHLIGHT_DIST = 5
    var LABEL_DIST_Y = 5
    var LABEL_MAX_WIDTH = "10em"

    // Model
    var canvas_topics               // topics displayed on canvas (Object, key: topic ID, value: CanvasTopic)
    var canvas_assocs               // relations displayed on canvas (Object, key: assoc ID, value: CanvasAssoc)
    var trans_x, trans_y            // canvas translation (in pixel)
    var highlight_topic_id          // ID of the highlighted topic (drawn with red frame), if any
    var grid_positioning            // while grid positioning is in progress: a GridPositioning object, null otherwise

    // View (Canvas)
    var ctx                         // the canvas drawing context

    // Short-term Interaction State (model)
    var topic_move_in_progress      // true while topic move is in progress (boolean)
    var canvas_move_in_progress     // true while canvas translation is in progress (boolean)
    var relation_in_progress        // true while new association is pulled (boolean)
    var action_topic                // the topic being moved / related (a CanvasTopic)
    var tmp_x, tmp_y                // coordinates while action is in progress

    // build the canvas
    init_model()
    create_canvas_element()
    $("#canvas-panel").dblclick(dblclick)
    $("#canvas-panel").mousemove(mousemove)
    $("#canvas-panel").mouseleave(mouseleave)

    // ------------------------------------------------------------------------------------------------------ Public API

    // === Overriding TopicmapRenderer Adapter Methods ===

    /**
     * @param   highlight_topic     Optional: if true, the topic is highlighted.
     * @param   refresh_canvas      Optional: if true, the canvas is refreshed.
     * @param   x                   Optional
     * @param   y                   Optional
     */
    this.add_topic = function(topic, highlight_topic, refresh_canvas, x, y) {
        if (!topic_exists(topic.id)) {
            // init geometry
            if (x == undefined && y == undefined) {
                if (grid_positioning) {
                    var pos = grid_positioning.next_position()
                    x = pos.x
                    y = pos.y
                } else {
                    x = Math.floor(self.canvas_width  * Math.random()) - trans_x
                    y = Math.floor(self.canvas_height * Math.random()) - trans_y
                }
            }
            // update model
            var ct = new CanvasTopic(topic, x, y)
            add_topic(ct)
            // trigger hook
            dm3c.trigger_plugin_hook("post_add_topic_to_canvas", ct)
        }
        // highlight topic
        if (highlight_topic) {
            set_highlight_topic(topic.id)
        }
        // update GUI
        if (refresh_canvas) {
            this.refresh()
        }
    }

    this.add_association = function(assoc, refresh_canvas) {
        if (!association_exists(assoc.id)) {
            // update model
            var ca = new CanvasAssoc(assoc)
            add_association(ca)
            // trigger hook
            dm3c.trigger_plugin_hook("post_add_relation_to_canvas", ca)
        }
        // update GUI
        if (refresh_canvas) {
            this.refresh()
        }
    }

    this.update_topic = function(topic) {
        get_topic(topic.id).update(topic)
    }

    this.remove_topic = function(id, refresh_canvas, is_part_of_delete_operation) {
        // 1) update model
        var ct = remove_topic(id)
        // assertion
        if (!ct) {
            throw "remove_topic: topic not on canvas (" + id + ")"
        }
        // 2) update GUI
        ct.label_div.remove()
        if (refresh_canvas) {
            this.refresh()
        }
        // 3) trigger hook
        if (!is_part_of_delete_operation) {
            dm3c.trigger_plugin_hook("post_hide_topic_from_canvas", ct)
        }
    }

    /**
     * Removes a relation from the canvas (model) and optionally refreshes the canvas (view).
     * If the relation is not present on the canvas nothing is performed.
     *
     * @param   refresh_canvas  Optional - if true, the canvas is refreshed.
     */
    this.remove_association = function(id, refresh_canvas, is_part_of_delete_operation) {
        // 1) update model
        var ca = remove_association(id)
        // Note: it is not an error if the relation is not present on the canvas. This can happen
        // for prgrammatically deleted relations, e.g. when updating a data field of type "reference".
        if (!ca) {
            return
            // throw "remove_association: relation not on canvas (" + id + ")"
        }
        //
        if (dm3c.current_rel_id == id) {
            dm3c.current_rel_id = null
        }
        // 2) update GUI
        if (refresh_canvas) {
            this.refresh()
        }
        // 3) trigger hook
        if (!is_part_of_delete_operation) {
            dm3c.trigger_plugin_hook("post_hide_relation_from_canvas", ca)
        }
    }

    this.remove_all_associations_of_topic = function(topic_id, is_part_of_delete_operation) {
        var assoc_ids = assoc_ids_of_topic(topic_id)
        for (var i = 0; i < assoc_ids.length; i++) {
            this.remove_association(assoc_ids[i], false, is_part_of_delete_operation)   // refresh_canvas=false
        }
    }

    this.scroll_topic_to_center = function(topic_id) {
        var ct = get_topic(topic_id)
        scroll_to_center(ct.x + trans_x, ct.y + trans_y)
    }

    this.refresh = function() {
        draw()
    }

    this.close_context_menu = function() {
        close_context_menu()
    }

    this.begin_relation = function(topic_id, event) {
        relation_in_progress = true
        action_topic = get_topic(topic_id)
        //
        tmp_x = cx(event)
        tmp_y = cy(event)
        draw()
    }

    this.clear = function() {
        // update GUI
        translate(-trans_x, -trans_y)                       // reset translation
        $("#canvas-panel .canvas-topic-label").remove()     // remove label divs
        // update model
        init_model()
    }

    this.resize = function() {
        resize_canvas()
    }

    // --- Grid Positioning ---

    this.start_grid_positioning = function() {
        grid_positioning = new GridPositioning()
    }

    this.stop_grid_positioning = function() {
        grid_positioning = null
    }

    // ----------------------------------------------------------------------------------------------- Private Functions



    /***************/
    /*** Drawing ***/
    /***************/



    function draw() {
        ctx.clearRect(-trans_x, -trans_y, self.canvas_width, self.canvas_height)
        //
        draw_associations()
        //
        if (relation_in_progress) {
            draw_line(action_topic.x, action_topic.y, tmp_x - trans_x, tmp_y - trans_y, ASSOC_WIDTH, ACTIVE_COLOR)
        }
        //
        draw_topics()
    }

    function draw_topics() {
        ctx.lineWidth = ACTIVE_TOPIC_WIDTH
        ctx.strokeStyle = ACTIVE_COLOR
        iterate_topics(function(ct) {
            var w = ct.icon.width
            var h = ct.icon.height
            try {
                ctx.drawImage(ct.icon, ct.x - w / 2, ct.y - h / 2)
                // highlight
                if (highlight_topic_id == ct.id) {
                    ctx.strokeRect(ct.x - w / 2 - HIGHLIGHT_DIST, ct.y - h / 2 - HIGHLIGHT_DIST,
                                          w + 2 * HIGHLIGHT_DIST, h + 2 * HIGHLIGHT_DIST)
                }
            } catch (e) {
                dm3c.log("### ERROR at Canvas.draw_topics:\nicon.src=" + ct.icon.src + "\nicon.width=" + ct.icon.width +
                    "\nicon.height=" + ct.icon.height  + "\nicon.complete=" + ct.icon.complete
                    /* + "\n" + JSON.stringify(e) */)
            }
        })
    }

    function draw_associations() {
        iterate_associations(function(ca) {
            var ct1 = get_topic(ca.get_topic1_id())
            var ct2 = get_topic(ca.get_topic2_id())
            // assertion
            if (!ct1 || !ct2) {
                // TODO: deleted relations must be removed from all topicmaps.
                alert("ERROR in draw_associations: relation " + ca.id + " is missing a topic")
                // ### delete canvas_assocs[i]
                return
            }
            // hightlight
            if (dm3c.current_rel_id == ca.id) {
                draw_line(ct1.x, ct1.y, ct2.x, ct2.y, ACTIVE_ASSOC_WIDTH, ACTIVE_COLOR)
            }
            //
            draw_line(ct1.x, ct1.y, ct2.x, ct2.y, ASSOC_WIDTH, ASSOC_COLOR)
        })
    }

    function draw_line(x1, y1, x2, y2, width, color) {
        ctx.lineWidth = width
        ctx.strokeStyle = color
        ctx.beginPath()
        ctx.moveTo(x1, y1)
        ctx.lineTo(x2, y2)
        ctx.stroke()
    }



    /**********************/
    /*** Event Handling ***/
    /**********************/



    // === Mouse Events ===

    function mousedown(event) {
        if (dm3c.LOG_GUI) dm3c.log("Mouse down!")
        //
        if (event.which == 1) {
            tmp_x = cx(event)
            tmp_y = cy(event)
            //
            var ct = topic_by_position(event)
            if (ct) {
                action_topic = ct
            } else if (!relation_in_progress) {
                canvas_move_in_progress = true
            }
        }
    }

    function mousemove(event) {
        if (action_topic || canvas_move_in_progress) {
            if (relation_in_progress) {
                tmp_x = cx(event)
                tmp_y = cy(event)
            } else if (canvas_move_in_progress) {
                var x = cx(event)
                var y = cy(event)
                translate(x - tmp_x, y - tmp_y)
                tmp_x = x
                tmp_y = y
            } else {
                topic_move_in_progress = true
                var x = cx(event)
                var y = cy(event)
                action_topic.move_by(x - tmp_x, y - tmp_y)
                tmp_x = x
                tmp_y = y
            }
            draw()
        }
    }

    function mouseleave(event) {
        if (dm3c.LOG_GUI) dm3c.log("Mouse leave!")
        //
        if (relation_in_progress) {
            end_relation_in_progress()
            draw()
        } else if (topic_move_in_progress) {
            end_topic_move()
        } else if (canvas_move_in_progress) {
            end_canvas_move()
        }
        end_interaction()
    }

    function mouseup(event) {
        if (dm3c.LOG_GUI) dm3c.log("Mouse up!")
        //
        close_context_menu()
        //
        if (relation_in_progress) {
            end_relation_in_progress()
            //
            var ct = topic_by_position(event)
            if (ct) {
                var assoc = dm3c.create_association("dm3.core.association",
                    {topic_id: dm3c.selected_topic.id, role_type_uri: dm3c.selected_topic.type_uri},
                    {topic_id: ct.id,                  role_type_uri: ct.type_uri}
                )
                dm3c.canvas.add_association(assoc)
                select_topic(dm3c.selected_topic.id)
            } else {
                draw()
            }
        } else if (topic_move_in_progress) {
            end_topic_move()
        } else if (canvas_move_in_progress) {
            end_canvas_move()
        } else {
            var ct = topic_by_position(event)
            if (ct) {
                select_topic(ct.id)
            }
        }
        end_interaction()
    }

    function dblclick(event) {
        var ct = topic_by_position(event)
        if (ct) {
            dm3c.trigger_plugin_hook("topic_doubleclicked", ct)
        }
    }

    // ---

    function topic_by_position(event) {
        var x = cx(event, true)
        var y = cy(event, true)
        return iterate_topics(function(ct) {
            if (x >= ct.x - ct.width / 2 && x < ct.x + ct.width / 2 &&
                y >= ct.y - ct.height / 2 && y < ct.y + ct.height / 2) {
                //
                return ct
            }
        })
    }

    function assoc_by_position(event) {
        var x = cx(event, true)
        var y = cy(event, true)
        return iterate_associations(function(ca) {
            var ct1 = get_topic(ca.get_topic1_id())
            var ct2 = get_topic(ca.get_topic2_id())
            // bounding rectangle
            var aw2 = ASSOC_WIDTH / 2   // buffer to make orthogonal associations selectable
            var bx1 = Math.min(ct1.x, ct2.x) - aw2
            var bx2 = Math.max(ct1.x, ct2.x) + aw2
            var by1 = Math.min(ct1.y, ct2.y) - aw2
            var by2 = Math.max(ct1.y, ct2.y) + aw2
            var in_bounding = x > bx1 && x < bx2 && y > by1 && y < by2
            if (!in_bounding) {
                return
            }
            // gradient
            var g1 = (y - ct1.y) / (x - ct1.x)
            var g2 = (y - ct2.y) / (x - ct2.x)
            // dm3c.log(g1 + " " + g2 + " -> " + Math.abs(g1 - g2))
            //
            if (Math.abs(g1 - g2) < ASSOC_CLICK_TOLERANCE) {
                return ca
            }
        })
    }

    // ---

    function end_topic_move() {
        topic_move_in_progress = false
        // trigger hook
        dm3c.trigger_plugin_hook("post_move_topic_on_canvas", action_topic)
    }

    function end_canvas_move() {
        canvas_move_in_progress = false
    }

    function end_relation_in_progress() {
        relation_in_progress = false
    }

    function end_interaction() {
        // remove topic activation
        action_topic = null
        // remove assoc activation
        if (dm3c.current_rel_id) {
            dm3c.current_rel_id = null
            draw()
        }
    }



    // === Context Menu Events ===

    function contextmenu(event) {
        if (dm3c.LOG_GUI) dm3c.log("Contextmenu!")
        //
        close_context_menu()
        //
        var ct, ca
        if (ct = topic_by_position(event)) {
            select_topic(ct.id, true)
            // Note: only dm3c.selected_topic has the auxiliary attributes (the canvas topic has not)
            var commands = dm3c.get_topic_commands(dm3c.selected_topic, "context-menu")
        } else if (ca = assoc_by_position(event)) {
            dm3c.current_rel_id = ca.id
            draw()
            var commands = dm3c.get_association_commands(ca, "context-menu")
        } else {
            var x = cx(event, true)
            var y = cy(event, true)
            var commands = dm3c.get_canvas_commands(x, y, "context-menu")
        }
        //
        open_context_menu(commands, event)
        //
        return false
    }

    /**
     * @param   commands    Array of commands. May be empty. Must not be null/undefined.
     */
    function open_context_menu(commands, event) {
        if (!commands.length) {
            return
        }
        var contextmenu = $("<div>").addClass("contextmenu").css({
            position: "absolute",
            top:  event.layerY + "px",
            left: event.layerX + "px"
        })
        for (var i = 0, cmd; cmd = commands[i]; i++) {
            if (cmd.is_separator) {
                contextmenu.append("<hr>")
            } else {
                var handler = context_menu_handler(cmd.handler)
                var a = $("<a>").attr("href", "#").click(handler).text(cmd.label)
                contextmenu.append(a)
            }
        }
        $("#canvas-panel").append(contextmenu)

        function context_menu_handler(handler) {
            return function(event) {
                handler(event)
                dm3c.canvas.close_context_menu()
                return false
            }
        }
    }

    function close_context_menu() {
        // remove context menu
        $("#canvas-panel .contextmenu").remove()
    }



    // === Drag and Drop Events ===

    // Required. Otherwise we don't receive a drop.
    function dragover () {
        // Return false is Required. Otherwise we don't receive a drop.
        return false
    }

    function drop(e) {
        // e.preventDefault();  // Useful for debugging when exception is thrown before false is returned.
        dm3c.trigger_plugin_hook("process_drop", e.dataTransfer)
        return false
    }



    /**********************/
    /*** Helper Methods ***/
    /**********************/



    // === Model Helper ===

    function init_model() {
        canvas_topics = {}
        canvas_assocs = {}
        trans_x = 0, trans_y = 0
    }

    // ---

    function get_topic(id) {
        return canvas_topics[id]
    }

    function add_topic(ct) {
        canvas_topics[ct.id] = ct
    }

    function remove_topic(id) {
        var ct = get_topic(id)
        delete canvas_topics[id]
        return ct
    }

    function topic_exists(id) {
        return get_topic(id) != undefined
    }

    function iterate_topics(func) {
        for (var id in canvas_topics) {
            var ret = func(get_topic(id))
            if (ret) {
                return ret
            }
        }
    }

    // ---

    function get_association(id) {
        return canvas_assocs[id]
    }

    function add_association(ca) {
        canvas_assocs[ca.id] = ca
    }

    function remove_association(id) {
        var ca = get_association(id)
        delete canvas_assocs[id]
        return ca
    }

    function association_exists(id) {
        return get_association(id) != undefined
    }

    function iterate_associations(func) {
        for (var id in canvas_assocs) {
            var ret = func(get_association(id))
            if (ret) {
                return ret
            }
        }
    }

    // ---

    function assoc_ids_of_topic(topic_id) {
        var assoc_ids = []
        iterate_associations(function(ca) {
            if (ca.get_topic1_id() == topic_id || ca.get_topic2_id() == topic_id) {
                assoc_ids.push(ca.id)
            }
        })
        return assoc_ids
    }

    // ---

    function set_highlight_topic(topic_id) {
        highlight_topic_id = topic_id
    }



    // === GUI Helper ===

    function select_topic(topic_id, synchronous) {
        set_highlight_topic(topic_id)
        draw()
        if (synchronous) {
            dm3c.render_topic(topic_id)
        } else {
            setTimeout(dm3c.render_topic, 0, topic_id)
        }
    }

    // ---

    /**
     * Creates the HTML5 canvas element, binds the event handlers, and adds it to the document.
     *
     * Called in 2 situations:
     * 1) When the main GUI is build initially.
     * 2) When the canvas is resized interactively.
     */
    function create_canvas_element() {
        var canvas = document.createElement("canvas")
        var canvas_elem = $(canvas).attr({id: "canvas", width: self.canvas_width, height: self.canvas_height})
        $("#canvas-panel").append(canvas_elem)  // add to document
        ctx = canvas.getContext("2d")
        // bind event handlers
        canvas_elem.mousedown(mousedown)
        canvas_elem.mouseup(mouseup)
        canvas.oncontextmenu = contextmenu
        canvas.ondragover = dragover
        canvas.ondrop = drop
    }

    /**
     * Resizes the HTML5 canvas element.
     *
     * Called in 2 situations:
     * 1) The user resizes the main window.
     * 2) The user resizes the canvas (by moving the split pane's resizable-handle).
     *
     * @param   size    the new canvas size.
     */
    function resize_canvas() {
        if (dm3c.LOG_GUI) dm3c.log("Rebuilding canvas")
        // Note: we don't empty the entire canvas-panel to keep the resizable-handle element.
        $("#canvas-panel #canvas").remove()
        $("#canvas-panel .canvas-topic-label").remove()
        // Note: in order to resize the canvas element we must recreate it.
        // Otherwise the browsers would just distort the canvas rendering.
        create_canvas_element()
        ctx.translate(trans_x, trans_y)
        draw()
        rebuild_topic_labels()
    }

    function translate(tx, ty) {
        ctx.translate(tx, ty)
        move_topic_labels_by(tx, ty)
        trans_x += tx
        trans_y += ty
    }

    function move_topic_labels_by(tx, ty) {
         iterate_topics(function(ct) {
             ct.move_label_by(tx, ty)
         })
    }

    function rebuild_topic_labels() {
        iterate_topics(function(ct) {
             ct.build_label()
        })
    }

    function scroll_to_center(x, y) {
        if (x < 0 || x >= self.canvas_width || y < 0 || y >= self.canvas_height) {
            var dx = (self.canvas_width / 2 - x) / CANVAS_ANIMATION_STEPS
            var dy = (self.canvas_height / 2 - y) / CANVAS_ANIMATION_STEPS
            var animation_count = 0;
            var animation = setInterval(function() {
                translate(dx, dy)
                draw()
                if (++animation_count == CANVAS_ANIMATION_STEPS) {
                    clearInterval(animation)
                }
            }, 0)
        }
    }

    /**
     * Returns the x coordinate of the mouse event.
     *
     * @as_canvas_coordinate    false: returned as screen coordinate.
     *                          true: returned as canvas coordinate (involves canvas viewport).
     */
    function cx(event, as_canvas_coordinate) {
        if ($(event.target).hasClass("canvas-topic-label")) {
            var offset = $(event.target).position().left
        } else {
            var offset = 0
        }
        return event.layerX + (as_canvas_coordinate ? -trans_x : 0) + offset
    }

    function cy(event, as_canvas_coordinate) {
        if ($(event.target).hasClass("canvas-topic-label")) {
            var offset = $(event.target).position().top
        } else {
            var offset = 0
        }
        return event.layerY + (as_canvas_coordinate ? -trans_y : 0) + offset
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

    /**
     * Properties:
     *  x, y            Topic position. Represents the center of the topic's icon.
     *  width, height   Icon size.
     */
    function CanvasTopic(topic, x, y) {

        var ct = this   // Note: variable "self" is already in use (canvas reference)

        this.x = x
        this.y = y

        init(topic);
        build_label()

        this.move_to = function(x, y) {
            this.x = x
            this.y = y
            init_label_pos()
            this.label_div.css(label_position_css())
        }

        this.move_by = function(tx, ty) {
            this.x += tx
            this.y += ty
            this.move_label_by(tx, ty)
        }

        this.move_label_by = function(tx, ty) {
            this.label_x += tx
            this.label_y += ty
            this.label_div.css(label_position_css())
        }

        this.build_label = function() {
            build_label()
        }

        this.update = function(topic) {
            init(topic)
            this.label_div.text(this.label)
        }

        function init(topic) {
            ct.id       = topic.id
            ct.type_uri = topic.type_uri
            ct.label    = topic.value
            //
            ct.icon = dm3c.get_type_icon(topic.type_uri)
            var w = ct.icon.width
            var h = ct.icon.height
            ct.width = w
            ct.height = h
            // label div
            ct.lox = -w / 2                   // label offset
            ct.loy = h / 2 + LABEL_DIST_Y     // label offset
            init_label_pos()
        }

        function init_label_pos() {
            ct.label_x = ct.x + ct.lox + trans_x
            ct.label_y = ct.y + ct.loy + trans_y
        }

        /**
         * Called in 2 situations:
         * - the topic is build initially.
         * - all label div's are rebuild (in reaction of resizing the canvas).
         */
        function build_label() {
            // Note: we must add the label div to the document (along with text content and max-width
            // setting) _before_ the clipping is applied. Otherwise the clipping can't be calculated
            // because the size of the label div is unknown.
            ct.label_div = $("<div>").addClass("canvas-topic-label").text(ct.label)
            ct.label_div.css({"max-width": LABEL_MAX_WIDTH})
            ct.label_div.mouseup(mouseup)       // to not block mouse gestures when mouse-up over the label div
            $("#canvas-panel").append(ct.label_div)
            ct.label_div.css(label_position_css())
            // Note: we must add the label div as a canvas sibling. As a canvas child element it doesn't appear.
        }

        /**
         * Builds the CSS for positioning and clipping the label div.
         *
         * Called in 3 situations:
         * - the label div is build initially.
         * - the topic has moved.
         * - the label div has moved.
         */
        function label_position_css() {
            // 1) Positioning
            var css = {position: "absolute", top: ct.label_y + "px", left: ct.label_x + "px"}
            // 2) Clipping
            // Note: we do clip each label div instead of "overflow: hidden" for the context panel
            // because "overflow: hidden" only works with absolute positioning the context panel
            // which in turn has a lot of consequences, e.g. the context menu items doesn't
            // occupy the entire context menu width anymore and I don't know how to fix it.
            var lx = ct.label_x;
            var ly = ct.label_y;
            // Note: if the label div is completely out of sight we must set it to "display: none".
            // Otherwise the document would grow and produce window scrollbars.
            if (lx > self.canvas_width || ly > self.canvas_height) {
                css.display = "none"
            } else {
                var lw = ct.label_div.width()
                var lh = ct.label_div.height()
                var top = ly < 0 ? -ly + "px" : "auto"
                var bottom = ly + lh > self.canvas_height ? self.canvas_height - ly + "px" : "auto"
                var left = lx < 0 ? -lx + "px" : "auto"
                var right = lx + lw > self.canvas_width ? self.canvas_width - lx + "px" : "auto"
                css.clip = "rect(" + top + ", " + right + ", " + bottom + ", " + left + ")"
                css.display = "block"
            }
            //
            return css
        }
    }

    function CanvasAssoc(assoc) {
        this.id = assoc.id
        this.type_uri = assoc.type_uri
        this.role_1 = assoc.role_1
        this.role_2 = assoc.role_2

        this.get_topic1_id = function() {
            return this.role_1.topic_id
        }

        this.get_topic2_id = function() {
            return this.role_2.topic_id
        }
    }

    // ---

    function GridPositioning() {

        // Settings
        var GRID_DIST_X = 180           // 10em (see LABEL_MAX_WIDTH) + 20 pixel padding
        var GRID_DIST_Y = 80
        var START_X = 50 - trans_x
        var START_Y = 50
        var MIN_Y = -9999

        var item_count = 0
        var start_pos = find_start_postition()
        var grid_x = start_pos.x
        var grid_y = start_pos.y

        this.next_position = function() {
            var pos = {x: grid_x, y: grid_y}
            if (item_count == 0) {
                scroll_to_center(self.canvas_width / 2, pos.y + trans_y)
            }
            //
            advance_position()
            item_count++
            //
            return pos
        }

        function find_start_postition() {
            var max_y = MIN_Y
            iterate_topics(function(ct) {
                if (ct.y > max_y) {
                    max_y = ct.y
                }
            })
            var x = START_X
            var y = max_y != MIN_Y ? max_y + GRID_DIST_Y : START_Y
            return {x: x, y: y}
        }

        function advance_position() {
            if (grid_x + GRID_DIST_X + trans_x > self.canvas_width) {
                grid_x = START_X
                grid_y += GRID_DIST_Y
            } else {
                grid_x += GRID_DIST_X
            }
        }
    }
}
