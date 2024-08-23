package lq.hh.resume.services;

import org.apache.http.HttpResponse;

import java.util.HashMap;
import java.util.Map;

public class InfoService {

    //About statuses you can read here: https://tech.hh.ru/api/rendered-docs/docs/resumes.md.html#publish
    private static final Map<Integer, String> responseStatusMessages = new HashMap<Integer, String>() {
        private static final long serialVersionUID = -2254679450533136837L;
        {
            put(new Integer(429), "Resume update not available yet. Try again later (4 hours timeout)");
            put(new Integer(204), "Resume updated");
        }
    };

    public Integer getStatusCode(HttpResponse response){
        Integer statusCode = response.getStatusLine().getStatusCode();
        return statusCode;
    }

    public String getMessageByStatusCode(Integer statusCode){
         return responseStatusMessages.getOrDefault(statusCode, "Unknown status " + statusCode);
    }
}
