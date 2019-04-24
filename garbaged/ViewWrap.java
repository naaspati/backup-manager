import org.json.JSONException;
import org.json.JSONObject;

import javafx.scene.Parent;
import sam.backup.manager.JsonRequired;

private class ViewWrap {
        final Class<? extends Parent> cls;
        final String key;
        
        JSONObject json;

        @SuppressWarnings({ "unchecked"})
        public ViewWrap(String key, JSONObject json) throws ClassNotFoundException, JSONException {
            this.key = key;
            this.cls = (Class<? extends Parent>) Class.forName(json.getString("class"));
            this.json = json;
        }
        
        @Override
        public String toString() {
            return json == null ? "ViewWrap []" : (key+":"+ json.toString());
        }
        
        public Parent instance() {
            Parent instance = injector.instance(cls);
            
            if(instance instanceof JsonRequired)
                ((JsonRequired) instance).setJson(key, json);
            
            return instance; 
        }
    }
    