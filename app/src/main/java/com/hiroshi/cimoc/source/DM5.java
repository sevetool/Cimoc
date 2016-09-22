package com.hiroshi.cimoc.source;

import com.hiroshi.cimoc.core.manager.SourceManager;
import com.hiroshi.cimoc.core.parser.MangaParser;
import com.hiroshi.cimoc.core.parser.NodeIterator;
import com.hiroshi.cimoc.core.parser.SearchIterator;
import com.hiroshi.cimoc.model.Chapter;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.ImageUrl;
import com.hiroshi.cimoc.soup.Node;
import com.hiroshi.cimoc.utils.DecryptionUtils;
import com.hiroshi.cimoc.utils.StringUtils;

import java.util.LinkedList;
import java.util.List;

import okhttp3.Request;

/**
 * Created by Hiroshi on 2016/8/25.
 */
public class DM5 extends MangaParser {

    @Override
    public Request getSearchRequest(String keyword, int page) {
        String url = StringUtils.format("http://www.dm5.com/search?page=%d&title=%s", page, keyword);
        return new Request.Builder().url(url).build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        Node body = new Node(html);
        return new NodeIterator(body.list("div.midBar > div.item")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.attr("dt > p > a.title", "href", "/", 1);
                String title = node.text("dt > p > a.title");
                String cover = node.attr("dl > a > img", "src");
                String update = node.text("dt > p > span.date", 6, -7);
                String author = node.text("dt > a:eq(2)");
                // boolean status = "已完结".equals(node.text("dt > p > span.date > span.red", 1, -2));
                return new Comic(SourceManager.SOURCE_DM5, cid, title, cover, update, author);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = "http://www.dm5.com/".concat(cid);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<Chapter> parseInfo(String html, Comic comic) {
        List<Chapter> list = new LinkedList<>();
        Node body = new Node(html);
        int count = 0;
        for (Node node : body.list("ul[id^=cbc_] > li > a")) {
            String c_title = node.text();
            try {
                String c_path = node.attr("href", "/", 1);
                if (count % 4 == 0) {
                    String[] array = c_title.split(" ", 2);
                    if (array.length == 2) {
                        c_title = array[1];
                    }
                }
                list.add(new Chapter(c_title, c_path));
            } catch (Exception e) {
                e.printStackTrace();
            }
            ++count;
        }

        String title = body.text("#mhinfo > div.inbt > h1.new_h2");
        String cover = body.attr("#mhinfo > div.innr9 > div.innr90 > div.innr91 > img", "src");
        String update = body.text("#mhinfo > div.innr9 > div.innr90 > div.innr92 > span:eq(9)", 5, -10);
        String author = body.text("#mhinfo > div.innr9 > div.innr90 > div.innr92 > span:eq(2) > a");
        String intro = body.text("#mhinfo > div.innr9 > div.mhjj > p").replace("[+展开]", "").replace("[-折叠]", "");
        boolean status = "已完结".equals(body.text("#mhinfo > div.innr9 > div.innr90 > div.innr92 > span:eq(6)", 5));
        comic.setInfo(title, cover, update, intro, author, status);

        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = "http://www.dm5.com/".concat(path);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
        String[] rs = StringUtils.match("var DM5_CID=(.*?);\\s*var DM5_IMAGE_COUNT=(\\d+);", html, 1, 2);
        if (rs != null) {
            String format = "http://www.dm5.com/m%s/chapterfun.ashx?cid=%s&page=%d";
            String packed = StringUtils.match("eval(.*?)\\s*</script>", html, 1);
            if (packed != null) {
                String key = StringUtils.match("comic=(.*?);", DecryptionUtils.evalDecrypt(packed), 1);
                if (key != null) {
                    key = key.replaceAll("'|\\+", "");
                    format = format.concat("&key=").concat(key);
                }
            }
            int page = Integer.parseInt(rs[1]);
            for (int i = 0; i != page; ++i) {
                list.add(new ImageUrl(i + 1, StringUtils.format(format, rs[0], rs[0], i + 1), true));
            }
        }
        return list;
    }

    @Override
    public Request getLazyRequest(String url) {
        return new Request.Builder().url(url).header("Referer", "http://www.dm5.com").build();
    }

    @Override
    public String parseLazy(String html, String url) {
        String result = DecryptionUtils.evalDecrypt(html);
        if (result != null) {
            return result.split(",")[0];
        }
        return null;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        return new Node(html).text("#mhinfo > div.innr9 > div.innr90 > div.innr92 > span:eq(9)", 5, -10);
    }

}