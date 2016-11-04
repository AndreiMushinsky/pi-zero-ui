(function($){
  
  var Renderer = function(elt){
    var dom = $(elt);
    var canvas = dom.get(0);
    var ctx = canvas.getContext("2d");
    var gfx = arbor.Graphics(canvas);
    var sys = null;

    var selected = null,
        nearest = null,
        _mouseP = null;

    
    var that = {
      init:function(pSystem){
        sys = pSystem;
        sys.screen({size:{width:dom.width(), height:dom.height()},
                    padding:[36,60,36,60]});

        $(window).resize(that.resize);
        that.resize();
        that._initMouseHandling();
      },
      resize:function(){
        canvas.width = $(window).width();
        canvas.height = .90* $(window).height();
        sys.screen({size:{width:canvas.width, height:canvas.height}});
        that.redraw();
      },
      redraw:function(){
        gfx.clear()
        sys.eachEdge(function(edge, p1, p2){
          if (edge.source.data.alpha * edge.target.data.alpha == 0) return
          gfx.line(p1, p2, {stroke:"#b2b19d", width:2, alpha:edge.target.data.alpha})
        })
        sys.eachNode(function(node, pt){
          var w = Math.max(12, 12+gfx.textWidth(node.data.name) ) * (node.data.center ? 1.6 : 1);
          var fs = 12 * (node.data.center ? 1.4 : 1);
          if (node.data.alpha===0) return
          if (node.data.shape=='dot'){
            gfx.oval(pt.x-w/2, pt.y-w/2, w, w, {fill:node.data.color, alpha:node.data.alpha})
            gfx.text(node.data.name, pt.x, pt.y+7, {color:"white", align:"center", font:"Arial", size:fs})
            gfx.text(node.data.name, pt.x, pt.y+7, {color:"white", align:"center", font:"Arial", size:fs})
          }else{
            gfx.rect(pt.x-w/2, pt.y-8, w, 20, 4, {fill:node.data.color, alpha:node.data.alpha})
            gfx.text(node.data.name, pt.x, pt.y+9, {color:"white", align:"center", font:"Arial", size:fs})
            gfx.text(node.data.name, pt.x, pt.y+9, {color:"white", align:"center", font:"Arial", size:fs})
          }
        })
      },

      switchMode:function(e){
        if (e.mode=='hidden'){
          dom.stop(true).fadeTo(e.dt,0, function(){
            if (sys) sys.stop()
            $(this).hide()
          })
        }else if (e.mode=='visible'){
          dom.stop(true).css('opacity',0).show().fadeTo(e.dt,1,function(){
            that.resize()
          })
          if (sys) sys.start()
        }
      },
      
      switchSection:function(newSection){
        var parent = sys.getEdgesFrom(newSection)[0].source
        var children = $.map(sys.getEdgesFrom(newSection), function(edge){
          return edge.target
        })
        
        sys.eachNode(function(node){
          if (node.data.shape=='dot') return; // skip all except leafnodes

          var nowVisible = ($.inArray(node, children)>=0)
          var newAlpha = (nowVisible) ? 1 : 0
          var dt = (nowVisible) ? .5 : .5
          sys.tweenNode(node, dt, {alpha:newAlpha})

          if (newAlpha==1){
            node.p.x = parent.p.x + .05*Math.random() - .025
            node.p.y = parent.p.y + .05*Math.random() - .025
            node.tempMass = .001
          }
        })
      },
      
      
      _initMouseHandling:function(){
        // no-nonsense drag and drop (thanks springy.js)
        selected = null;
        nearest = null;
        var dragged = null;
        var oldmass = 1

        var _section = null

        var handler = {
          moved:function(e){
            var pos = $(canvas).offset();
            _mouseP = arbor.Point(e.pageX-pos.left, e.pageY-pos.top)
            nearest = sys.nearest(_mouseP);

            if (!nearest.node) return false

            if (nearest.node.data.shape == "dot") {
              if (nearest.node.name!=_section){
                _section = nearest.node.name
                that.switchSection(_section)
              }
              dom.removeClass('linkable')
              window.status = ''
            }
            
            return false
          },
          clicked:function(e){
            var pos = $(canvas).offset();
            _mouseP = arbor.Point(e.pageX-pos.left, e.pageY-pos.top)
            nearest = dragged = sys.nearest(_mouseP);
            
            if (nearest && selected && nearest.node===selected.node){
              var link = selected.node.data.link
              if (link.match(/^#/)){
                 $(that).trigger({type:"navigate", path:link.substr(1)})
              }else{
                 window.location = link
              }
              return false
            }
            
            
            if (dragged && dragged.node !== null) dragged.node.fixed = true

            $(canvas).unbind('mousemove', handler.moved);
            $(canvas).bind('mousemove', handler.dragged)
            $(window).bind('mouseup', handler.dropped)

            return false
          },
          dragged:function(e){
            var old_nearest = nearest && nearest.node._id
            var pos = $(canvas).offset();
            var s = arbor.Point(e.pageX-pos.left, e.pageY-pos.top)

            if (!nearest) return
            if (dragged !== null && dragged.node !== null){
              var p = sys.fromScreen(s)
              dragged.node.p = p
            }

            return false
          },

          dropped:function(e){
            if (dragged===null || dragged.node===undefined) return
            if (dragged.node !== null) dragged.node.fixed = false
            dragged.node.tempMass = 1000
            dragged = null;
            // selected = null
            $(canvas).unbind('mousemove', handler.dragged)
            $(window).unbind('mouseup', handler.dropped)
            $(canvas).bind('mousemove', handler.moved);
            _mouseP = null
            return false
          }


        }

        $(canvas).mousedown(handler.clicked);
        $(canvas).mousemove(handler.moved);

      }
    }
    
    return that
  }
  
  $(document).ready(function(){
    
    var schema = {
        "index":"physical",
        "type":"CommodityPhysicalAsset",
        "properties":{
          "CommodityPhysicalAsset":{
            "properties":{
              "AdminStatus":{"type":"string"},
              "AssetAliasName":{"type":"string"},
              "AssetCommodityType":{
                "properties":{
                  "AssetCommodityType":{"type":"string"},
                  "AssetCommodityTypeID":{"type":"string"},
                  "AssetCommodityTypeIO":{"type":"string"},
                  "AssetCommodityTypeRCS":{"type":"string"},
                  "AssetCommodityTypeRank":{"type":"string"},
                  "effectiveFrom":{"type":"date","format":"strict_date_optional_time||epoch_millis"},
                  "languageId":{"type":"string"},
                  "relatedObjectId":{"type":"string"},
                  "relatedObjectType":{"type":"string"},
                  "relatedObjectTypeId":{"type":"string"},
                  "relationObjectType":{"type":"string"},
                  "relationObjectTypeId":{"type":"string"},
                  "relationshipType":{"type":"string"},
                  "relationshipTypeId":{"type":"string"}}},
              "AssetCoordinate":{
                "properties":{
                  "AssetCoordinateAccuracy":{"type":"string"},
                  "AssetCoordinatePrecision":{"type":"string"},
                  "AssetCoordinateSupplier":{
                    "properties":{
                      "AssetCoordinateSupplier":{"type":"string"},
                      "AssetCoordinateSupplierID":{"type":"string"},
                      "effectiveFrom":{"type":"date","format":"strict_date_optional_time||epoch_millis"},
                      "languageId":{"type":"string"},
                      "relatedObjectId":{"type":"string"},
                      "relatedObjectType":{"type":"string"},
                      "relatedObjectTypeId":{"type":"string"},
                      "relationObjectType":{"type":"string"},
                      "relationObjectTypeId":{"type":"string"},
                      "relationshipType":{"type":"string"},
                      "relationshipTypeId":{"type":"string"}}},
                  "AssetCoordinateType":{"type":"string"},
                  "AssetLatitude":{"type":"string"},
                  "AssetLongitude":{"type":"string"},
                  "effectiveFrom":{"type":"date","format":"strict_date_optional_time||epoch_millis"},
                  "languageId":{"type":"string"}}},
              "AssetExName":{"type":"string"},
              "AssetFullName":{"type":"string"},
              "AssetGeographicUnit":{
                "properties":{
                  "AssetGeographicUnit":{"type":"string"},
                  "AssetGeographicUnitID":{"type":"string"},
                  "AssetGeographicUnitRCS":{"type":"string"},
                  "AssetGeographicUnitRank":{"type":"string"},
                  "AssetGeographicUnitType":{"type":"string"},
                  "effectiveFrom":{"type":"date","format":"strict_date_optional_time||epoch_millis"},
                  "languageId":{"type":"string"},
                  "relatedObjectId":{"type":"string"},
                  "relatedObjectType":{"type":"string"},
                  "relatedObjectTypeId":{"type":"string"},
                  "relationObjectType":{"type":"string"},
                  "relationObjectTypeId":{"type":"string"},
                  "relationshipType":{"type":"string"},
                  "relationshipTypeId":{"type":"string"}}},
              "AssetID":{"type":"string"},
              "AssetOrganisation":{
                "properties":{
                  "AssetOrganisationID":{"type":"string"},
                  "AssetOrganisationName":{"type":"string"},
                  "AssetOrganisationPercentage":{"type":"string"},
                  "AssetOrganisationRank":{"type":"string"},
                  "AssetOrganisationRole":{"type":"string"},
                  "effectiveFrom":{"type":"date","format":"strict_date_optional_time||epoch_millis"},
                  "languageId":{"type":"string"},
                  "relatedObjectId":{"type":"string"},
                  "relatedObjectType":{"type":"string"},
                  "relatedObjectTypeId":{"type":"string"},
                  "relationObjectType":{"type":"string"},
                  "relationObjectTypeId":{"type":"string"},
                  "relationshipType":{"type":"string"},
                  "relationshipTypeId":{"type":"string"}}},
              "AssetPermissionableObject":{"type":"string"},
              "AssetRank":{"type":"string"},
              "AssetType":{
                "properties":{
                  "AssetStatus":{
                    "properties":{
                      "AssetStatusType":{"type":"string"},
                      "AssetStatusTypeValue":{"type":"string"},
                      "languageId":{"type":"string"}}},
                  "AssetType":{"type":"string"},
                  "AssetTypeID":{"type":"string"},
                  "AssetTypeRCS":{"type":"string"},
                  "effectiveFrom":{"type":"date","format":"strict_date_optional_time||epoch_millis"},
                  "languageId":{"type":"string"},
                  "relatedObjectId":{"type":"string"},
                  "relatedObjectType":{"type":"string"},
                  "relatedObjectTypeId":{"type":"string"},
                  "relationObjectType":{"type":"string"},
                  "relationObjectTypeId":{"type":"string"},
                  "relationshipType":{"type":"string"},
                  "relationshipTypeId":{"type":"string"}}},
              "AssetZoom":{
                "properties":{
                  "AssetZoomRegions":{"type":"string"},
                  "AssetZoomRegionsBoundsLat":{"type":"string"},
                  "AssetZoomRegionsBoundsLon":{"type":"string"},
                  "AssetZoomRegionsRCS":{"type":"string"}}},
              "effectiveFrom":{"type":"date","format":"strict_date_optional_time||epoch_millis"},
              "effectiveTo":{"type":"date","format":"strict_date_optional_time||epoch_millis"},
              "languageId":{"type":"string"},
              "objectType":{"type":"string"},
              "objectTypeId":{"type":"string"}}}}};
    
    
    var id = 0;
    var newUI = { nodes:{}, edges:{} };

    var CLR = {
        center: "#000033",
        branch:"#9D937F",
        long:"orange",
        date:"#922E00",
        string:"#a7af00"
    };
    
    function process(parentId, node, nodeName) {
      $.each(node, function(key, val) {
        if (key == "properties") {
          var nodeId = ++id;
          var isCenter = parentId == 0;
          if (!isCenter) addEdge(parentId, nodeId, {length:.2});
          addNode({"id": nodeId, name:nodeName, color:(isCenter? CLR.center: CLR.branch), center:isCenter, shape:"dot", alpha:1});
          $.each(val, function(childName, child) {
            process(nodeId, child, childName);
          });
        } else if (key == "type") {
          var nodeId = ++id;
          addNode({"id": nodeId, name:nodeName, color:CLR[val], alpha:0});
          addEdge(parentId, nodeId, {length:.4});
        };
      });
    };
    
    function addNode( node ) {
      newUI.nodes[node.id.toString()] = node;
      newUI.edges[node.id.toString()] = {};
    };
    
    function addEdge( parentId, childId, edge ) {
      newUI.edges[parentId.toString()][childId.toString()] = edge;
    }
 
    process(id, schema.properties[schema.type], schema.type);

    var sys = arbor.ParticleSystem()
    sys.parameters({stiffness:900, repulsion:1200, gravity:false, dt:0.015})
    sys.renderer = Renderer("#sitemap")
    sys.graft(newUI)

  })
})(this.jQuery)