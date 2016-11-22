/**
 * Created by andreas on 02.05.14.
 */
avnav.provide('avnav.gui.AisInfoPage');

var navobjects=require('../nav/navobjects');
var AisHandler=require('../nav/aisdata');


/**
 *
 * @constructor
 */
avnav.gui.AisInfoPage=function(){
    avnav.gui.Page.call(this,'aisinfopage',
        {
            eventlist:[navobjects.NavEvent.EVENT_TYPE],
            returnOnClick: true
        }
    );
    /**
     * @private
     * @type {AisHandler}
     */
    this.aishandler=null;

    /**
     * @private
     * @type {undefined}
     */
    this.mmsi=undefined;
    /**
     * private
     * @type {string}
     */
    this.statusItem='.avn_Status';

    this.currentStatus=undefined;
};
avnav.inherits(avnav.gui.AisInfoPage,avnav.gui.Page);

avnav.gui.AisInfoPage.prototype.localInit=function(){
    this.aishandler=this.navobject.getAisHandler();
};
avnav.gui.AisInfoPage.prototype.showPage=function(options) {
    if (!this.gui) return;
    this.mmsi=options?options.mmsi:undefined;
    if (this.mmsi === undefined) {
        var current=this.aishandler.getNearestAisTarget();
        if (current) this.mmsi=current.mmsi;
    }
    this.fillData(true);
};

avnav.gui.AisInfoPage.prototype.getCurrentTarget=function(){
    var current=this.aishandler.getAisByMmsi(this.mmsi);
    var warning=this.aishandler.getNearestAisTarget();
    if (warning && warning.warning){
        this.mmsi=warning.mmsi;
        return warning;
    }
    return current;
};
avnav.gui.AisInfoPage.prototype.fillData=function(initial){
    var currentObject=this.getCurrentTarget();
    if (! this.aishandler || currentObject === undefined){
        this.returnToLast();
        return;
    }
    var status='normal';
    if (currentObject.warning){
        status='warning';
    }
    else{
        if (currentObject.nearest) status='nearest';
    }
    if (status != this.currentStatus) {
        this.selectOnPage(this.statusItem).attr('src',this.gui.map.getAisIcon(status));
        this.currentStatus=status;
    }
    this.selectOnPage(this.statusItem).css('transform','rotate('+currentObject.course+'deg)');

    var self=this;
    this.selectOnPage(".avn_infopage_inner").show();
    this.selectOnPage(".avn_ais_data").each(function(idx,el){
        var name=$(this).attr('data-name');
        if (! name) return;
        var val=self.aishandler.formatAisValue(name,currentObject);
        $(this).text(val);
    });


};


avnav.gui.AisInfoPage.prototype.hidePage=function(){

};
//-------------------------- Buttons ----------------------------------------

avnav.gui.AisInfoPage.prototype.btnAisInfoNearest=function (button,ev){
    this.aishandler.setTrackedTarget(0);
    var pos=this.aishandler.getAisPositionByMmsi(this.aishandler.getTrackedTarget());
    if (pos)this.gui.map.setCenter(pos);
    this.returnToLast();
    avnav.log("Nearest clicked");
};

avnav.gui.AisInfoPage.prototype.btnAisInfoList=function (button,ev){
    avnav.log("List clicked");
    this.aishandler.setTrackedTarget(this.mmsi);
    this.gui.showPage('aispage',{skipHistory:true});
};
avnav.gui.AisInfoPage.prototype.btnAisInfoLocate=function (button,ev){
    avnav.log("Locate clicked");
    if (this.mmsi === undefined) return;
    var pos=this.aishandler.getAisPositionByMmsi(this.mmsi);
    if (pos)this.gui.map.setCenter(pos);
    this.gui.map.setGpsLock(false);
    this.returnToLast();
};
/**
 * create the page instance
 */
(function(){
    //create an instance of the status page handler
    var page=new avnav.gui.AisInfoPage();
}());

