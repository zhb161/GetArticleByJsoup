import io.github.furstenheim.CodeBlockStyle;
import io.github.furstenheim.CopyDown;
import io.github.furstenheim.Options;
import io.github.furstenheim.OptionsBuilder;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpUtil;

public class GetArticle {
    public static void main(String[] args) throws IOException, InterruptedException {
        String all=initData.all;
        // 待爬取的专栏地址
        String[] split = all.split(",");
        String directory = initData.directory;
        for (String s : split) {
            getAllColoum(directory,s);

        }


    }

    private static void getAllColoum(String directory, String zhuanlanDirectory) throws IOException, InterruptedException {
        List<String> coloumArticleNames = getColoumArticleNames(directory, zhuanlanDirectory);
        for (String coloumArticleName : coloumArticleNames) {
            //
            getArticleAndPicturn(directory, zhuanlanDirectory, coloumArticleName);
        }
    }

    public static List<String> getColoumArticleNames(String directory,String zhuanlanDirectory) throws IOException, InterruptedException {
        System.out.println("----------正在爬取" +  zhuanlanDirectory + "下的文章");
        Thread.sleep(21000);
        Connection connect = Jsoup.connect(directory + "/" + zhuanlanDirectory).timeout(30000);
        Document document = connect.get();
        //获取document中class为book-menu uncollapsible 的标签 的第二个子标签
        Elements elementsByClass = document.getElementsByClass("book-menu uncollapsible");
        Elements uncollapsible = elementsByClass.get(0).getElementsByClass("menu-item");
        //获取uncollapsible中所有标签的id值并转为字符串数组

        List<String> ids = new ArrayList<>();
        for (Element element : uncollapsible) {
            String id = element.id();
            if (!id.isEmpty()) { // 确保id不为空
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * 获取文章内容，并下载图片
     *
     * @param directory         专栏的目录
     * @param zhuanlanDirectory 专栏名
     * @param fileName          文章名
     */
    private static void getArticleAndPicturn(String directory, String zhuanlanDirectory, String fileName) throws IOException, InterruptedException {

        System.out.println( "----------正在爬取" + fileName );
        String file="D:\\document\\爬取文件/" + zhuanlanDirectory + "/" + fileName;
        //如果文件已经存在，则直接返回
        if(FileUtil.exist(file)){
            return;
        }

        Thread.sleep(21000);
        Document document = Jsoup.connect(directory + "/" + zhuanlanDirectory + "/" + fileName).get();
        //获取document中class为book-post 的标签的所有子标签，并转为字符串
        String content = document.getElementsByClass("book-post").toString();
        String contentMD = htmlTansToMarkdown(content);
        //网址的最后一个斜杠后的字符串为文件名，string类型的contentMD为文件内容，默认为UTF-8编码，放在resources文件夹中，使用hutool包完成这个功能
        // 写入文件
        FileUtil.writeString(contentMD, "D:\\document\\爬取文件/" + zhuanlanDirectory + "/" + fileName, CharsetUtil.UTF_8);
        //写入图片
        String[] assets = extractAssets(contentMD);
        for (String asset : assets) {
            //等待10秒，避免反爬虫
            Thread.sleep(21000);
            if(!"".equals(asset)){
                downloadImage(directory + zhuanlanDirectory + "/" + asset, "D:\\document\\爬取文件/" + zhuanlanDirectory + "/assets", asset.substring(7, asset.length()));
            }
        }
    }

    public static String htmlTansToMarkdown(String htmlStr) {
        OptionsBuilder optionsBuilder = OptionsBuilder.anOptions();
        Options options = optionsBuilder.withBr("-").withCodeBlockStyle(CodeBlockStyle.FENCED)
                // more options
                .build();
        CopyDown converter = new CopyDown(options);
        return converter.convert(htmlStr);
    }


    //windows 系统下文件名不能包含以下字符：\ / : * ? " < > | 替换为 _
    public static String sanitizeFileName(String fileName) {
        String invalidChars = "\\/:*?\"<>|"; // Windows 不允许的文件名字符
        for (char c : invalidChars.toCharArray()) {
            fileName = fileName.replace(c, '_'); // 将非法字符替换为下划线
        }
        return fileName;
    }

    /**
     * 提取字符串中所有以(assets/开头，)结尾的部分，并去除最左和最右边的（）
     *
     * @param input 输入字符串
     * @return 匹配的结果数组
     */
    public static String[] extractAssets(String input) {
        // 正则表达式匹配 (assets/...) 的部分
        String regex = "\\(assets/(.*?)\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        // 存储匹配结果
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            sb.append(trimFirstAndLastChar(matcher.group(0))).append(",");
        }

        // 将结果转换为数组
        String[] assets = sb.toString().split(",");
        return assets;
    }

    /**
     * 去掉字符串的第一个和最后一个字符（用于去掉图片前后的括号）
     *
     * @param str 输入字符串
     * @return 去掉第一个和最后一个字符后的字符串
     */
    public static String trimFirstAndLastChar(String str) {
        if (str == null || str.length() <= 2) {
            return "";
        }
        return str.substring(1, str.length() - 1);
    }

    /**
     * 下载图片并保存到本地文件夹
     *
     * @param imageUrl        图片的 URL 地址
     * @param outputDirectory 保存图片的目录
     * @param fileName        图片的文件名
     */

    public static void downloadImage(String imageUrl, String outputDirectory, String fileName) {
        try {
            // 创建输出文件路径
            String outputPath = outputDirectory + "/" + fileName;

            // 发起 HTTP GET 请求并获取响应字节流
            System.out.println("正在下载图片: " + imageUrl);

            long size = HttpUtil.downloadFile(imageUrl, FileUtil.file(outputPath));


            System.out.println("图片下载成功: " + fileName);
        } catch (Exception e) {
            System.err.println("图片下载失败: " + e.getMessage());
        }
    }
}
