/**
 * Created by Andreas on 27.04.2014.
 */

avnav.provide('avnav.gui.Handler');
avnav.provide('avnav.gui.PageEvent');
/**
 * the page change event
 * @param {avnav.gui.Handler} gui
 * @param {avnav.nav.NavObject} navobject
 * @param {string} oldpage
 * @param {string} newpage
 * @param {object} opt_options
 * @constructor
 * @extends {
 */
avnav.gui.PageEvent=function(gui,navobject,oldpage,newpage,opt_options){
    this.gui=gui;
    this.navobject=navobject;
    this.oldpage=oldpage;
    this.newpage=newpage;
    this.options=opt_options;
};
/**
 * the type for the page event
 * @type {string}
 * @const
 */
avnav.gui.PageEvent.EVENT_TYPE='cangepage';


/**
 *
 * @param {avnav.util.PropertyHandler} properties
 * @param {avnav.nav.NavObject} navobject
 * @param {ol.Map} map
 * @constructor
 */
avnav.gui.Handler=function(properties,navobject,map){
    /** {avnav.util.PropertyHandler} */
    this.properties=properties;
    /** {avnav.nav.NavObject} */
    this.navobject=navobject;
    /** {avnav.map.MapHolder} */
    this.map=map;
};

/**
 * show a certain page
 * @param {String} name
 * @param {Object} options options to be send as options with the event
 * @returns {boolean}
 */

avnav.gui.Handler.prototype.showPage=function(name,options){
    if (! name) return false;
    if (name == this.page) return false;
    $('.avn_page').hide();
    $('#avi_'+name).show();
    var oldname=this.page;
    this.page=name;
    log("trigger page event");
    $(document).trigger(avnav.gui.PageEvent.EVENT_TYPE, new avnav.gui.PageEvent(
        this,
        this.navobject,
        oldname,
        name,
        options
    ));

};

