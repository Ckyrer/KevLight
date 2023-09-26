package ru.kvdl.kevlight;

public interface Action {
    void response(String[] headers, String ip, Responser responser);
}
