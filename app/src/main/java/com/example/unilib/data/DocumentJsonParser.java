package com.example.unilib.data;

import com.example.unilib.model.DocStatus;
import com.example.unilib.model.Document;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class DocumentJsonParser {
    private DocumentJsonParser() { }

    public static List<Document> parse(String body) throws JSONException {
        List<Document> list = new ArrayList<>();
        JSONArray array = new JSONArray(body);
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) continue;
            JSONObject doc = item.optJSONObject("document");
            if (doc == null) continue;                  // empty result: only "readTime"
            JSONObject fields = doc.optJSONObject("fields");
            if (fields == null) continue;

            Document d = new Document();
            d.setId(lastSegment(doc.optString("name", "")));
            d.setTitle(text(fields, "title"));
            d.setCourseCode(text(fields, "courseCode"));
            d.setDescription(text(fields, "description"));
            d.setFileUrl(text(fields, "fileUrl"));
            d.setUploaderName(text(fields, "uploaderName"));
            d.setFileSize(number(fields, "fileSize"));
            d.setStatus(DocStatus.APPROVED);
            if (!d.getTitle().isEmpty() && !d.getFileUrl().isEmpty()) list.add(d);   // skip broken records
        }
        return list;
    }

    private static String text(JSONObject fields, String key) {
        JSONObject v = fields.optJSONObject(key);
        return v == null ? "" : v.optString("stringValue", "");
    }

    private static long number(JSONObject fields, String key) {
        JSONObject v = fields.optJSONObject(key);
        return v == null ? 0L : v.optLong("integerValue", 0L);
    }

    private static String lastSegment(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }
}