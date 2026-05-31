package com.example.a23110035_23110060;

import android.text.Html;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EpubExtractor {

    public interface EpubCallback {
        void onSuccess(List<Chapter> chapters);
        void onError(Exception e);
    }

    public static void fetchAndParseEpub(String url, File cacheDir, EpubCallback callback) {
        new Thread(() -> {
            try {
                // 1. Download EPUB
                File epubFile = new File(cacheDir, "temp_book.epub");
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) throw new Exception("Download failed");
                    try (InputStream is = response.body().byteStream();
                         FileOutputStream fos = new FileOutputStream(epubFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }

                // 2. Unzip and parse in memory
                Map<String, byte[]> files = new HashMap<>();
                try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(epubFile)))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (!entry.isDirectory()) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buffer = new byte[8192];
                            int count;
                            while ((count = zis.read(buffer)) != -1) {
                                baos.write(buffer, 0, count);
                            }
                            files.put(entry.getName(), baos.toByteArray());
                        }
                        zis.closeEntry();
                    }
                }

                // 3. Find content.opf from META-INF/container.xml
                byte[] containerXml = files.get("META-INF/container.xml");
                if (containerXml == null) throw new Exception("Invalid EPUB: no container.xml");
                
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document containerDoc = dBuilder.parse(new ByteArrayInputStream(containerXml));
                NodeList rootfiles = containerDoc.getElementsByTagName("rootfile");
                if (rootfiles.getLength() == 0) throw new Exception("Invalid EPUB: no rootfile");
                String opfPath = ((Element) rootfiles.item(0)).getAttribute("full-path");

                // 4. Parse content.opf
                byte[] opfData = files.get(opfPath);
                if (opfData == null) throw new Exception("OPF file not found: " + opfPath);
                
                String opfDir = "";
                if (opfPath.contains("/")) {
                    opfDir = opfPath.substring(0, opfPath.lastIndexOf('/') + 1);
                }

                Document opfDoc = dBuilder.parse(new ByteArrayInputStream(opfData));
                
                // Read manifest
                Map<String, String> manifest = new HashMap<>();
                NodeList itemNodes = opfDoc.getElementsByTagName("item");
                for (int i = 0; i < itemNodes.getLength(); i++) {
                    Element item = (Element) itemNodes.item(i);
                    manifest.put(item.getAttribute("id"), item.getAttribute("href"));
                }

                // Read spine
                List<Chapter> chapters = new ArrayList<>();
                NodeList itemrefNodes = opfDoc.getElementsByTagName("itemref");
                for (int i = 0; i < itemrefNodes.getLength(); i++) {
                    Element itemref = (Element) itemrefNodes.item(i);
                    String idref = itemref.getAttribute("idref");
                    String href = manifest.get(idref);
                    
                    if (href != null) {
                        String filePath = opfDir + href;
                        byte[] htmlData = files.get(filePath);
                        if (htmlData != null) {
                            String htmlString = new String(htmlData, "UTF-8");
                            
                            // Clean HTML: Remove style and script tags before converting to text
                            String cleanHtml = htmlString.replaceAll("(?s)<style.*?>.*?</style>", "")
                                                         .replaceAll("(?s)<script.*?>.*?</script>", "");
                            
                            String plainText;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                plainText = Html.fromHtml(cleanHtml, Html.FROM_HTML_MODE_LEGACY).toString().trim();
                            } else {
                                plainText = Html.fromHtml(cleanHtml).toString().trim();
                            }
                            
                            // Try to extract a better title from the HTML content
                            String title = "Chương " + (i + 1);
                            
                            // 1. Try to find H1 or H2 tags first (they usually have the real chapter name)
                            java.util.regex.Pattern headerPattern = java.util.regex.Pattern.compile("<h[12][^>]*>(.*?)</h[12]>", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL);
                            java.util.regex.Matcher mHeader = headerPattern.matcher(htmlString);
                            if (mHeader.find()) {
                                String hText = Html.fromHtml(mHeader.group(1)).toString().trim();
                                if (!hText.isEmpty() && hText.length() < 100) {
                                    title = hText;
                                }
                            } else {
                                // 2. Fallback to <title> tag but filter out generic ones like "index"
                                java.util.regex.Matcher m = java.util.regex.Pattern.compile("<title>(.*?)</title>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(htmlString);
                                if (m.find()) {
                                    String extractedTitle = m.group(1);
                                    if (extractedTitle != null) {
                                        String t = extractedTitle.trim();
                                        if (!t.isEmpty() && !t.equalsIgnoreCase("index") && !t.equalsIgnoreCase("untitled") && !t.toLowerCase().contains(".html")) {
                                            title = t;
                                        }
                                    }
                                }
                            }

                            // Filter out pages that are too short or likely just metadata (like Cover images)
                            // Usually, meaningful text content is longer than 30 characters
                            if (!plainText.isEmpty() && plainText.length() > 30) {
                                Chapter chapter = new Chapter();
                                chapter.setChapterNumber(i + 1);
                                chapter.setTitle(title);
                                chapter.setTextContent(plainText);
                                chapters.add(chapter);
                            }
                        }
                    }
                }

                if (chapters.isEmpty()) throw new Exception("No chapters found in EPUB");

                // Clean up
                epubFile.delete();

                callback.onSuccess(chapters);

            } catch (Exception e) {
                Log.e("EpubExtractor", "Error parsing EPUB", e);
                callback.onError(e);
            }
        }).start();
    }
}
