package com.zlikun.learning.dao;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * 数据持久化到Mongo
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2017/12/3 16:36
 */
public class NovelDao {

    private static String host = "mongo.zlikun.com" ;
    private static int port = 27017 ;

    private static MongoClient client;
    private static MongoDatabase database;
    private static MongoCollection<Document> collection;

    private static NovelDao INSTANCE = new NovelDao();

    static {
        client = new MongoClient(host, port);
        database = client.getDatabase("novels");
        collection = database.getCollection("chapters");
    }

    public static NovelDao getInstance() {
        return INSTANCE;
    }

    public void save(String book, Long number, String title, String content) {
        collection.insertOne(new Document()
                .append("book", book)
                .append("number", number)
                .append("title", title)
                .append("content", content));
    }

}
