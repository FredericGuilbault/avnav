package de.wellenvogel.avnav.appapi;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import de.wellenvogel.avnav.fileprovider.UserFileProvider;
import de.wellenvogel.avnav.main.Constants;
import de.wellenvogel.avnav.main.R;
import de.wellenvogel.avnav.main.WebViewFragment;
import de.wellenvogel.avnav.util.AvnLog;

//potentially the Javascript interface code is called from the Xwalk app package
//so we have to be careful to always access the correct resource manager when accessing resources!
//to make this visible we pass a resource manager to functions called from here that open dialogs
public class JavaScriptApi {
    private UploadData uploadData=null;
    private RequestHandler requestHandler;
    private WebViewFragment fragment;
    private boolean detached=false;

    public JavaScriptApi(WebViewFragment fragment, RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
        this.fragment=fragment;
    }

    public void saveFile(Uri uri){
        if (uploadData != null) uploadData.saveFile(uri);
    }

    public void onDetach(){
        detached=true;
        if (uploadData != null){
            uploadData.interruptCopy(true);
        }
    }

    private String returnStatus(String status){
        JSONObject o=new JSONObject();
        try {
            o.put("status", status);
        }catch (JSONException i){}
        return o.toString();
    }

    @JavascriptInterface
    public boolean downloadFile(String name, String type,String url) {
        if (detached) return false;
        if (requestHandler.typeDirs.get(type) == null) {
            AvnLog.e("invalid type " + type + " for sendFile");
            return false;
        }
        Uri data = null;
        if (type.equals("layout")) {
            data = LayoutHandler.getUriForLayout(name);
        } else {
            try {
                data = UserFileProvider.createContentUri(type, name,url);
            } catch (Exception e) {
                AvnLog.e("unable to create content uri for " + name);
            }
        }
        if (data == null) return false;
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, data);
        shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (type.equals("layout")) {
            shareIntent.setType("application/json");
        } else {
            shareIntent.setType("application/gpx+xml");
        }
        String title = requestHandler.activity.getText(R.string.selectApp) + " " + name;
        requestHandler.activity.startActivity(Intent.createChooser(shareIntent, title));
        return true;
    }

    /**
     * replacement for the missing ability to intercept post requests
     * will only work for small data provided here as a string
     * basically it calls the request handler and returns the status as string
     * @param url
     * @param data
     * @return status
     */
    @JavascriptInterface
    public String handleUpload(String url,String data){
        if (detached) return null;
        try {
            JSONObject rt= requestHandler.handleUploadRequest(Uri.parse(url),new PostVars(data));
            return rt.getString("status");
        } catch (Exception e) {
            AvnLog.e("error in upload request for "+url+":",e);
            return e.getMessage();
        }

    }

    /**
     * request a file from the system
     * this is some sort of a workaround for the not really working onOpenFileChooser
     * in the onActivityResult (MainActivity) we do different things depending on the
     * "readFile" flag
     * If true we store the selected file and its name in the uploadData structure
     * we limit the size of the file to 1M - this should be ok for our routes, layouts and similar
     * the results will be retrieved via getFileName and getFileData
     * when the file successfully has been fetched, we fire an {@link Constants#JS_UPLOAD_AVAILABLE} event to the JS with the id
     *
     * If false, we only store the Uri and wait for {@link #copyFile}
     * In this case we fire an {@link Constants#JS_FILE_COPY_READY}
     * the id is some minimal security to ensure that somenone later on can only access the file
     * when knowing this id
     * a new call to this API will empty/overwrite any older data being retrieved
     * @param type one of the user data types (route|layout)
     * @param id to identify the file
     * @param readFile if true: read the file (used for small files), otherwise jsut keep the file url for later copy
     */
    @JavascriptInterface
    public boolean requestFile(String type,int id,boolean readFile){
        if (detached) return false;
        RequestHandler.KeyValue<Integer> title= RequestHandler.typeHeadings.get(type);
        if (title == null){
            AvnLog.e("unknown type for request file "+type);
            return false;
        }
        if (uploadData != null) uploadData.interruptCopy(true);
        if (fragment.getActivity() == null) return false;
        uploadData=new UploadData(fragment, requestHandler.getHandler(type),id,readFile);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        Resources res=fragment.getResources();
        try {
            fragment.startActivityForResult(
                    Intent.createChooser(intent,
                            //TODO: be more flexible for types...
                            res.getText(title.value)),
                    Constants.FILE_OPEN);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(fragment.getActivity(), res.getText(R.string.installFileManager), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @JavascriptInterface
    public String getFileName(int id){
        if (uploadData==null || ! uploadData.isReady(id)) return null;
        return uploadData.getName();
    }

    @JavascriptInterface
    public String getFileData(int id){
        if (uploadData==null || ! uploadData.isReady(id)) return null;
        return uploadData.getFileData();
    }

    /**
     * start a file copy operation for a file that previously has been requested by
     * {@link #requestFile(String, int, boolean)}
     * during the transfer we fire an {@link Constants#JS_FILE_COPY_PERCENT} event having the
     * progress in % as id.
     * When done we fire {@link Constants#JS_FILE_COPY_DONE} - with 0 for success, 1 for errors
     * The target is determined by the type that we provided in requestFile
     * @param id the id we used in {@link #requestFile(String, int, boolean)}
     * @return true if the copy started successfully
     */
    @JavascriptInterface
    public boolean copyFile(int id){
        if (detached) return false;
        if (uploadData==null || ! uploadData.isReady(id)) return false;
        return uploadData.copyFile();
    }
    @JavascriptInterface
    public long getFileSize(int id){
        if (uploadData==null || ! uploadData.isReady(id)) return -1;
        return uploadData.getSize();
    }

    @JavascriptInterface
    public boolean interruptCopy(int id){
        if (uploadData==null || ! uploadData.isReady(id)) return false;
        return uploadData.interruptCopy(false);
    }


    @JavascriptInterface
    public String setLeg(String legData){
        if (detached) return null;
        if (requestHandler.getRouteHandler() == null) return returnStatus("not initialized");
        try {
            requestHandler.getRouteHandler().setLeg(legData);
            requestHandler.getGpsService().timerAction();
            return returnStatus("OK");
        } catch (Exception e) {
            AvnLog.i("unable to save leg "+e.getLocalizedMessage());
            return returnStatus(e.getMessage());
        }
    }

    @JavascriptInterface
    public String unsetLeg(){
        if (detached) return null;
        if (requestHandler.getRouteHandler() == null) return returnStatus("not initialized");
        try {
            requestHandler.getRouteHandler().unsetLeg();
            requestHandler.getGpsService().timerAction();
            return returnStatus("OK");
        } catch (Exception e) {
            AvnLog.i("unable to unset leg "+e.getLocalizedMessage());
            return returnStatus(e.getMessage());
        }
    }

    @JavascriptInterface
    public void goBack() {
        if (detached) return;
        requestHandler.activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        requestHandler.activity.goBack();
                    }
                });
    }

    @JavascriptInterface
    public void acceptEvent(String key,int num){
        if (detached) return;
        if (key != null && key.equals("backPressed")) {
           fragment.jsGoBackAccepted(num);
        }
    }

    @JavascriptInterface
    public void showSettings(){
        if (detached) return;
        requestHandler.activity.showSettings(false);
    }

    @JavascriptInterface
    public void applicationStarted(){
        if (detached) return;
        requestHandler.getSharedPreferences().edit().putBoolean(Constants.WAITSTART,false).commit();
    }
    @JavascriptInterface
    public void externalLink(String url){
        if (detached) return;
        Intent goDownload = new Intent(Intent.ACTION_VIEW);
        goDownload.setData(Uri.parse(url));
        try {
            requestHandler.activity.startActivity(goDownload);
        } catch (Exception e) {
            Toast.makeText(requestHandler.activity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            return;
        }
    }
    @JavascriptInterface
    public String getVersion(){
        if (detached) return null;
        try {
            String versionName = requestHandler.activity.getPackageManager()
                    .getPackageInfo(requestHandler.activity.getPackageName(), 0).versionName;
            return versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "<unknown>";
        }
    }
    @JavascriptInterface
    public boolean dimScreen(int percent){
        fragment.setBrightness(percent);
        return true;
    }


}
