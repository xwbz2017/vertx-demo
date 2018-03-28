package me.xwbz.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * 语音朗读
 * Created by hasee on 2018/1/10.
 * Demo：
 * 1.	学拼读
 * a)	拆分音标，分别读音标及原单词
 * b)	根据拆分的单词，给两个发音选项，选择正确的发音
 * 2.	绘本
 * a)	以我们的方式高亮文字
 * b)	可针对单词另外读及翻译
 * 2-3天完成demo，最后发布到180服务器上看
 * 1.	两个页面分别加各自的链接，能分别跳转另外个页面
 * 2.	两个页面空白处加上备注，备注内容：说明我们目前做到的功能，有哪些缺陷
 * 3.	发布到180上，能在手机访问
 */
public class SpeakVerticle extends AbstractVerticle {

    private String tempDir = System.getProperty("java.io.tmpdir");

    private final static Map<String, String> FILE_AUDIO_MAP = new ConcurrentHashMap<>();

    private final static Map<String, String> FILE_MAP = new ConcurrentHashMap<>();

    private final static Map<String, JsonObject> FILEOBJ_MAP = new ConcurrentHashMap<>();

    private final static Map<String, JsonObject> PHONETIC_MAP = new ConcurrentHashMap<>();

    private final static Map<String, Double> FILE_LEN_MAP = new ConcurrentHashMap<>();

    private static final String YIN_FILE_DIR = "audio";

    private final static List<String> AUDIO_FILE_LIST = new ArrayList<>();

    private final static String[] YIN = {"i[:ː]", "ɪ", "e", "æ", "ɑ[:ː]", "ɒ", "ɔ[:ː]", "[ʊu]", "uː", "ʌ", "ɜː", "ə", // 元音
            "eɪ", "aɪ", "ɔɪ", "əʊ", "aʊ", "ɪə", "eə", "ʊə",
            "p", "b", "t", "d", "k", "[ɡg]", "f", "v", "s", "z", "θ", "ð", "ʃ", "ʒ", "tʃ", "dʒ", // 辅音
            "tr", "dr", "ts", "dz", "m", "l", "n", "ŋ", "h", "r", "w", "j"};

    private FileSystem fs;
    private WebClient client;
    private Random random = new Random();

    private static String urlEncode(String str) {
        String result = null;
        try {
            result = URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void run(RoutingContext context) {
        client.get("openapi.baidu.com", "/oauth/2.0/token")
                .addQueryParam("grant_type", "client_credentials")
                .addQueryParam("client_id", urlEncode(BaiduSpeak.appKey))
                .addQueryParam("client_secret", urlEncode(BaiduSpeak.secretKey))
                .send(res -> {
                    if (res.succeeded()) {
                        JsonObject obj = res.result().bodyAsJsonObject();
                        BaiduSpeak.token = obj.getString("access_token");
                        System.out.println(obj.toString());
                        context.response().end(BaiduSpeak.token);
                    } else {
                        res.cause().printStackTrace();
                    }
                });
        System.out.println("ok");
    }

    private void body(RoutingContext context) {
        String txt = context.request().getParam("txt");
        if (FILE_AUDIO_MAP.containsKey(txt)) {
            context.response().end(FILE_AUDIO_MAP.get(txt));
            return;
        }
        Future<HttpResponse<Buffer>> getFuture = Future.future();
        Future<String> fileFuture = Future.future();

        client.get("tsn.baidu.com", "/text2audio")
                .addQueryParam("tex", urlEncode(txt))
                .addQueryParam("per", BaiduSpeak.per + "")
                .addQueryParam("spd", BaiduSpeak.spd + "")
                .addQueryParam("pit", BaiduSpeak.pit + "")
                .addQueryParam("vol", BaiduSpeak.vol + "")
                .addQueryParam("cuid", BaiduSpeak.cuid + "")
                .addQueryParam("tok", BaiduSpeak.token)
                .addQueryParam("lan", "zh")
                .addQueryParam("ctp", "1")
                .send(getFuture.completer());

        getFuture.compose(h -> {
            String fkey = UUID.randomUUID().toString().replace("-", "") + ".mp3";
            String fname = tempDir + File.separator + fkey;

            fs.writeFile(fname, h.body(), rr -> {
                if (rr.succeeded()) {
                    FILE_AUDIO_MAP.put(txt, fkey);
                    fileFuture.complete(fkey);
                } else {
                    rr.cause().printStackTrace();
                    fileFuture.fail(rr.cause());
                }
            });
        }, fileFuture);

        fileFuture.setHandler(r -> {
            if (r.succeeded()) {
                context.response().end(r.result());
            } else {
                context.response().end(r.cause().getMessage());
            }
        });
    }

    private void body2(RoutingContext context) {
        String txt = context.request().getParam("txt");
        if (FILEOBJ_MAP.containsKey(txt)) {
            context.response().putHeader("content-type", "application/json;charset=utf-8").end(FILEOBJ_MAP.get(txt).toBuffer());
            return;
        }
        Future<HttpResponse<Buffer>> getFuture = Future.future();
        Future<String> fileFuture = Future.future();

        client.get("tsn.baidu.com", "/text2audio")
                .addQueryParam("tex", urlEncode(txt))
                .addQueryParam("per", BaiduSpeak.per + "")
                .addQueryParam("spd", BaiduSpeak.spd + "")
                .addQueryParam("pit", BaiduSpeak.pit + "")
                .addQueryParam("vol", BaiduSpeak.vol + "")
                .addQueryParam("cuid", BaiduSpeak.cuid + "")
                .addQueryParam("tok", BaiduSpeak.token)
                .addQueryParam("lan", "zh")
                .addQueryParam("ctp", "1")
                .send(getFuture.completer());

        getFuture.compose(h -> {
            String fkey = UUID.randomUUID().toString().replace("-", "") + ".mp3";
            String fname = tempDir + File.separator + fkey;

            fs.writeFile(fname, h.body(), rr -> {
                if (rr.succeeded()) {
                    try {
                        MP3File f = (MP3File) AudioFileIO.read(new File(fname));
                        MP3AudioHeader audioHeader = (MP3AudioHeader) f.getAudioHeader();
                        System.out.println(fkey + " 播放长度(s) -> " + audioHeader.getPreciseTrackLength());
                        FILE_LEN_MAP.put(txt, audioHeader.getPreciseTrackLength());
                        fileFuture.complete(fkey);
                    } catch (Exception e) {
                        fileFuture.fail(e.getCause());
                    }
                } else {
                    rr.cause().printStackTrace();
                    fileFuture.fail(rr.cause());
                }
            });
        }, fileFuture);

        fileFuture.setHandler(r -> {
            if (r.succeeded()) {
                JsonObject res = new JsonObject()
                        .put("audio", r.result())
                        .put("split", cut(txt));
                FILEOBJ_MAP.put(txt, res);
                context.response().putHeader("content-type", "application/json;charset=utf-8").end(res.toBuffer());
            } else {
                context.response().end(r.cause().getMessage());
            }
        });
    }

    private List<String[]> cut(String txt) {
//        String[] arr = txt.split("/[,.，。‘’“”'\"\\r\\n]/");
        Double total = FILE_LEN_MAP.get(txt);
        String[] arr = txt.split("\\W");
        List<String[]> wordList = new ArrayList<>(arr.length);
        double idx = 0;
        for (String t : arr) {
            if (t != null) {
                double duration = total * t.length() / txt.length() * 1000;
                if (random.nextBoolean()) {
                    wordList.add(new String[]{t, idx + "", duration + "", "这是翻译"});
                } else {
                    wordList.add(new String[]{t, idx + "", duration + ""});
                }
                idx += duration;
            }
        }
        return wordList;
    }

    private void html(RoutingContext context) {
        String path = context.request().getParam("path");
        fs.readFile(path + ".html", res -> {
            if (res.succeeded()) {
                context.response()
                        .putHeader("content-type", "text/html;charset=utf-8")
                        .end(res.result());
            } else {
                res.cause().printStackTrace();
            }
        });
    }

    private void audio(RoutingContext context) {
        String fkey = context.request().getParam("key");
        String fname = tempDir + File.separator + fkey;
        fs.readFile(fname, res -> {
            if (res.succeeded()) {
                context.response()
                        .putHeader("content-type", "audio/mpeg")
                        .end(res.result());
            } else {
                res.cause().printStackTrace();
                context.response().end("文件未找到");
            }
        });
    }

    private void search(RoutingContext context) {
        String word = context.request().getParam("word"),
                beginStr = "<span class=\"phonetic\">",
                endStr = "</span>";
        Future<HttpResponse<Buffer>> getFuture = Future.future();
        Future<JsonObject> phoneticFuture = Future.future();

        Future<HttpResponse<Buffer>> audioFuture = Future.future();
        Future<String> fileFuture = Future.future();

        getFuture.setHandler(r -> {
            if (r.succeeded()) {
                String html = r.result().bodyAsString();
                JsonObject phoneticResult = new JsonObject();
                if (html.contains(beginStr)) {
                    String phonetic = html.substring(html.indexOf(beginStr) + beginStr.length());
                    phonetic = phonetic.substring(0, phonetic.indexOf(endStr));
                    System.out.println(word + " -> 获取到音标：" + phonetic);

                    phoneticResult.put("origin", phonetic);
                    // 多个读音只保留第一个 phonetic.split(";")[0]
                    phonetic = position(phonetic.split(";")[0], YIN.length - 1);
                    System.out.println(word + " -> 音标位置信息：" + phonetic);
                    List<String[]> splitAudios = toAudioFile(phonetic);

                    phoneticResult.put("splitAudios", splitAudios);
                    phoneticResult.put("concatAudio", concatAudio(splitAudios));

                    PHONETIC_MAP.put(word, phoneticResult);
                    phoneticFuture.complete(phoneticResult);
                } else {
                    System.out.println(word + " -> 无音标信息");
                    // 不管音标成功还是失败，还是得继续
                    phoneticFuture.complete(phoneticResult);
                }
            } else {
                r.cause().printStackTrace();
                phoneticFuture.fail(r.cause());
            }
        });

        audioFuture.setHandler(h -> {
            if (h.succeeded()) {
                String fkey = UUID.randomUUID().toString().replace("-", "") + ".mp3";
                String fname = tempDir + File.separator + fkey;
                fs.writeFile(fname, h.result().bodyAsBuffer(), rr -> {
                    if (rr.succeeded()) {
                        System.out.println(word + " -> 文件: " + fkey);
                        FILE_MAP.put(word, fkey);
                        fileFuture.complete(fkey);
                    } else {
                        rr.cause().printStackTrace();
                        fileFuture.fail(rr.cause());
                    }
                });
            } else {
                h.cause().printStackTrace();
            }
        });

        HttpServerResponse response = context.response().putHeader("content-type", "application/json");

        CompositeFuture.join(phoneticFuture, fileFuture).setHandler(h -> {
            if (h.succeeded()) {
                JsonObject result = new JsonObject();
                JsonObject phonetic = phoneticFuture.result();
                if (phonetic.isEmpty()) {
                    result.put("error", "无音标信息");
                } else {
                    result.put("phonetic", phonetic);
                }
                result.put("speakAudio", fileFuture.result());
                response.end(result.toBuffer());
            } else {
                h.cause().printStackTrace();
                context.response().end("error");
            }
        });

        if (PHONETIC_MAP.containsKey(word)) {
            phoneticFuture.complete(PHONETIC_MAP.get(word));
        } else {
            System.out.println("获取音标：" + word);
            client.get("dict.youdao.com", "/search")
                    .addQueryParam("q", word)
                    .send(getFuture.completer());
        }
        if (FILE_MAP.containsKey(word)) {
            fileFuture.complete(FILE_MAP.get(word));
        } else {
            System.out.println("获取文件：" + word);
            client.get("dict.youdao.com", "/dictvoice")
                    .addQueryParam("audio", word)
                    .addQueryParam("type", "1")
                    .send(audioFuture.completer());
        }
    }

    private List<String[]> toAudioFile(final String positions) {
        String temp = positions;
        List<String[]> files = new ArrayList<>();
        while (temp.contains("{")) {
            int idx = Integer.parseInt(temp.substring(temp.indexOf("{") + 1, temp.indexOf("}")));
            int rIdx = randomYIN(idx);

            files.add(new String[]{AUDIO_FILE_LIST.get(idx), AUDIO_FILE_LIST.get(rIdx)});

            temp = temp.replaceFirst("[{]" + idx + "[}]", "");
        }
        return files;
    }

    private String concatAudio(List<String[]> audioFiles) {
        String fkey = UUID.randomUUID().toString().replace("-", "") + ".mp3";
        String fname = tempDir + File.separator + fkey;
        Buffer buffer = Buffer.buffer();

        for (String[] file : audioFiles) {
            buffer.appendBuffer(fs.readFileBlocking(YIN_FILE_DIR + File.separator + file[0]));
        }
        fs.writeFileBlocking(fname, buffer);
        System.out.println("已合并音频文件：" + fkey);
        return fkey;
    }

    private void init() {
        Future<List<String>> future = Future.future();
        final String keySuffix = "_k";

        System.out.println("预计加载音标文件：" + YIN.length);

        fs.readDir(YIN_FILE_DIR, future.completer());

        future.setHandler(h -> {
            if (h.succeeded()) {
                Map<String, String> fileMap = new HashMap<>();

                h.result().forEach(f -> {
                    String file = f.substring(f.lastIndexOf(File.separator) + File.separator.length());
                    fileMap.put(file.substring(file.lastIndexOf("_") + 1, file.lastIndexOf(".mp3")) + keySuffix, file);
                });

                List<Integer> sorted = fileMap.keySet().stream().mapToInt(k -> Integer.valueOf(k.replace(keySuffix, "")))
                        .sorted().boxed().collect(Collectors.toList());

                for (Integer idx : sorted) {
                    String k = idx + keySuffix;
                    AUDIO_FILE_LIST.add(fileMap.get(k));
                }
                System.out.println("已加载本地音标文件：" + AUDIO_FILE_LIST.size());
            } else {
                h.cause().printStackTrace();
            }
        });
    }

    private int randomYIN(int y) {
        int[] idxArr = new int[YIN.length - 1];
        for (int i = YIN.length - 1, j = 0; i >= 0; i--) {
            if (i == y) continue;
            idxArr[j++] = i;
        }
        return idxArr[random.nextInt(idxArr.length)];
    }

    private String position(String phonetic, int idx) {
        // 需要反着遍历
        if (idx < 0) return phonetic;
        return position(phonetic.replaceAll(YIN[idx], "{" + idx + "}"), idx - 1);
    }

    @Override
    public void start() {
        fs = vertx.fileSystem();
        client = WebClient.create(vertx);
        init();

        // 实例化一个路由器出来，用来路由不同的rest接口
        Router router = Router.router(vertx);
        // 增加一个处理器，将请求的上下文信息，放到RoutingContext中
        router.route().handler(BodyHandler.create());
        // 处理一个post方法的rest接口
        router.get("/init").handler(this::run);
        // 将访问“/audio/*”的请求route到“audio”目录下的资源
        router.route("/audio/:key").handler(this::audio);
        router.get("/search/:word").handler(this::search);
        router.route("/static/*").handler(StaticHandler.create("audio"));
        // 处理一个get方法的rest接口
        router.get("/body/:txt").handler(this::body);
        router.get("/body2/:txt").handler(this::body2);
        router.get("/html/:path").handler(this::html);
        // 创建一个httpserver，监听80端口，并交由路由器分发处理用户请求
        vertx.createHttpServer().requestHandler(router::accept).listen(80);
        System.out.println(this.getClass().getName() + "正在监听80端口");
    }


    static class BaiduSpeak {
        /**
         * 填写网页上申请的appkey
         */
        private static final String appKey = "x8h9aDqSgi8bgcRUB0rdK6lz";

        /**
         * 填写网页上申请的APP SECRET
         */
        private static final String secretKey = "48a78a95f5041554a64186077cca6d2f";

        /**
         * 发音人选择, 0为普通女声，1为普通男生，3为情感合成-度逍遥，4为情感合成-度丫丫，默认为普通女声
         */
        private static final int per = 4;
        /**
         * 语速，取值0-9，默认为5中语速
         */
        private static final int spd = 5;
        /**
         * 音调，取值0-9，默认为5中语调
         */
        private static final int pit = 5;
        /**
         * 音量，取值0-9，默认为5中音量
         */
        private static final int vol = 5;

        private static String cuid = "1234567JAVA";

        private static String token = "24.408588bcdb510f5761b6ca0e7cd724e0.2592000.1524236901.282335-10972844";
    }
}
