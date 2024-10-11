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
        Connection connect = Jsoup.connect(directory + "/" + zhuanlanDirectory).timeout(30000);
        Document document = connect.get();
        Thread.sleep(31000);
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
    private static void getArticleAndPicturn(String directory, String zhuanlanDirectory, String fileName) throws InterruptedException {
        System.out.println("----------正在处理文章：" + fileName);
        String filePath = "D:\\document\\爬取文件\\" + zhuanlanDirectory + "\\" + fileName;

        // 如果文件已经存在，跳过文章抓取，进入图片检查
        if (FileUtil.exist(filePath)) {
            System.out.println("文章已存在，跳过抓取：" + fileName);
            try {
                checkAndDownloadImages(directory, zhuanlanDirectory, fileName);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            return;
        }

        // 如果文章文件不存在，开始抓取文章内容
        Document document = null;
        try {
            document = Jsoup.connect(directory + "/" + zhuanlanDirectory + "/" + fileName).get();
        } catch (IOException e) {
            System.out.println("抓取文章失败，跳过：" + fileName);
            return;
        }
        Thread.sleep(31000);
        if(document==null){
            System.out.println("抓取文章失败，跳过：" + fileName);
            return;
        }
        String content = document.getElementsByClass("book-post").toString();
        String contentMD = htmlTansToMarkdown(content);

        // 写入文章内容到本地
        FileUtil.writeString(contentMD, filePath, CharsetUtil.UTF_8);
        System.out.println("文章抓取完成并保存：" + fileName);

        // 处理文章中的图片
        try {
            checkAndDownloadImages(directory, zhuanlanDirectory, fileName);
        } catch (Exception e) {
            System.out.println(e.getMessage());

        }
    }
    private static void checkAndDownloadImages(String directory, String zhuanlanDirectory, String fileName) throws IOException, InterruptedException {
        String filePath = "D:\\document\\爬取文件\\" + zhuanlanDirectory + "\\" + fileName;
        String contentMD = FileUtil.readString(filePath, CharsetUtil.UTF_8);

        // 提取Markdown中的图片链接
        String[] assets = extractAssets(contentMD);
        for (String asset : assets) {
            if (!"".equals(asset)) {
                String imagePath = "D:\\document\\爬取文件\\" + zhuanlanDirectory + "\\assets\\" + asset.substring(7);
                // 检查图片是否已经存在
                if (!FileUtil.exist(imagePath)) {
                    // 如果图片不存在，则下载图片
                    downloadImage(directory + zhuanlanDirectory + "/" + asset, "D:\\document\\爬取文件\\" + zhuanlanDirectory + "\\assets", asset.substring(7));
                } else {
                    System.out.println("图片已存在，跳过下载：" + asset);
                }
            }
        }}
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

    public static void downloadImage(String imageUrl, String outputDirectory, String fileName) throws InterruptedException {
        try {
            // 创建输出文件路径
            String outputPath = outputDirectory + "/" + fileName;

            // 发起 HTTP GET 请求并获取响应字节流
            System.out.println("正在下载图片: " + imageUrl);

            long size = HttpUtil.downloadFile(imageUrl, FileUtil.file(outputPath));
            System.out.println("图片下载成功: " + fileName);
            Thread.sleep(31000);


        } catch (Exception e) {
            System.err.println("图片下载失败: " + e.getMessage());
        }
    }
}
