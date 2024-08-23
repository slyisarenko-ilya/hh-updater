package lq.hh.resume.services;

import org.apache.http.HttpResponse;

import java.io.IOException;

public interface HttpService {

    String get(String command, String accessToken) throws IOException ;

    void put(String command, String body, String accessToken) throws IOException;

    HttpResponse post(String command, String accessToken) throws IOException;

}
