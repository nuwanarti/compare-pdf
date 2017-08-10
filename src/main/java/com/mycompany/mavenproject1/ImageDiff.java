/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.mavenproject1;

import gui.ava.html.image.generator.HtmlImageGenerator;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.fit.pdfdom.PDFDomTree;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 *
 * @author rt
 * @prerequest python3 and opencv numpy installed
 * @Os -> ubuntu 16.04 LTS
 *
 * @summary -> if there are any text available in the pdf then text will be
 * compared and results will be sored in result.html file
 *
 * @summary -> differences between the images will be stored as result.png
 *
 * removed text from pdf and saved the pdf as html successfully but couldn't
 * convert the html into image so couldn't compare text free image files
 *
 *
 */
public class ImageDiff implements CustomCallback {

    /**
     *
     * @param {string} fileLoc -> pdf file URI 1
     * @param {String} fileLoc -> pdf file URI 2
     * @param {String} savingDir -> directory to save the image files
     *
     */
    static ArrayList<Pdf> pdfList = new ArrayList<>();

    public static void main(String[] args) {
        try {
            //run the heavy work simultaneously
            runInBack("/home/rt/NetBeansProjects/mavenproject1"
                    + "/masterDOC1.pdf",
                    "/home/rt/NetBeansProjects/mavenproject1/");
            runInBack("/home/rt/NetBeansProjects/mavenproject1"
                    + "/duplicateDOC1.pdf",
                    "/home/rt/NetBeansProjects/mavenproject1/");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void runRest() {
        Pdf pdf1 = pdfList.get(0);
        Pdf pdf2 = pdfList.get(1);
//            fileLoc = "/home/rt/NetBeansProjects/mavenproject1"
//                    + "/duplicateDOC1.pdf";
//            Pdf pdf2 = new Pdf(fileLoc, savingDir);

        String text1 = pdf1.string;
        String text2 = pdf2.string;
        String result = "<html><head></head><body><h2>original text (pdf 1)</h2>"
                + text1
                + "<h2>duplicated text (pdf2)</h2>"
                + text2
                + "<h1>Corrected Text</h1>"
                + getString(text2, text1) + "</body></html>";
        System.out.println(result);
        saveHTMLfile(result);
        try {
            callPythonTodifferentiateImages(pdf1.imageUri, pdf2.imageUri);
        } catch (IOException ex) {
            Logger.getLogger(ImageDiff.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void runInBack(String fileLoc, String savingDir) {
        class RunInBack implements Runnable {

            Pdf pdf;
            String fileLoc;
            String savingDir;
            CustomCallback c = new ImageDiff();

            public RunInBack(String fileLoc, String savingDir) {
                this.pdf = pdf;
                this.fileLoc = fileLoc;
                this.savingDir = savingDir;
            }

            @Override
            public void run() {
                try {
                    pdf = new Pdf(fileLoc, savingDir);
                } catch (IOException ex) {
                    Logger.getLogger(ImageDiff.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ParserConfigurationException ex) {
                    Logger.getLogger(ImageDiff.class.getName()).log(Level.SEVERE, null, ex);
                }
                    c.returnPdf(pdf);
            }

            public Pdf returnPdf() {
                return pdf;
            }

        }
        RunInBack r = new RunInBack(fileLoc, savingDir);
        Thread t = new Thread(r);
        t.start();

    }

    public static void saveHTMLfile(String text) {
        try {
            PrintWriter writer = new PrintWriter("result.html", "UTF-8");
            writer.println(text);
            writer.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * calling python to handle the differences between the images
     *
     * @param image1 -> name of the first image to be compared
     * @param image2 -> name of the second image to be compared if the images
     * are in the same directory
     * @throws IOException
     */
    public static void callPythonTodifferentiateImages(String image1, String image2) throws IOException {
        String command = "python3 image_diff.py --first " + image1 + " --second " + image2;
        Process proc = Runtime.getRuntime().exec(command);
        // Read the output
        BufferedReader reader
                = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line = "";
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        try {
            proc.waitFor();
        } catch (InterruptedException ex) {
            System.out.println(ex.getMessage());
        }
        System.out.println(reader.toString());
    }

    /**
     * if text has been INSERTED then inserted fraction will be red color if
     * text has been DELETED then deleted fraction will be blue
     *
     * @param s1 -> string of the first pdf
     * @param s2 -> string of the second pdf
     * @return result HTML string
     */
    public static String getString(String s1, String s2) {
        diff_match_patch dmp = new diff_match_patch();
        dmp.Diff_Timeout = 0;
        LinkedList<diff_match_patch.Diff> d = dmp.diff_main(s1, s2, false);
        String fullText = "";
        for (diff_match_patch.Diff c : d) {
            if (c.operation == diff_match_patch.Operation.EQUAL) {
                fullText += c.text;
            } else if (c.operation == diff_match_patch.Operation.INSERT) {
                fullText += "<span style='color:red'>" + c.text + "</span>";
            } else {
                fullText += "<span style='color:blue'>" + c.text + "</span>";
            }
        }
        return fullText;
    }

    @Override
    public void returnPdf(Pdf pdf) {
        pdfList.add(pdf);
        if(pdfList.size() == 2)
            runRest();
//        else
//            System.out.println("size of the list is : "+ pdfList.size());
    }

    public static class Pdf {

        private String pdf;
        private String string;
        private String imageUri;

        public Pdf(String fileLoc, String savingDir) throws IOException, ParserConfigurationException {
            this.pdf = fileLoc;
            extractDetails(fileLoc, savingDir);

        }

        /**
         * extracting text and image from the pdf file
         *
         * @param fileLoc -> location of the pdf file
         * @param savingDir -> destination directory
         * @throws IOException
         * @throws ParserConfigurationException
         */
        public void extractDetails(String fileLoc, String savingDir) throws IOException, ParserConfigurationException {
            PDDocument pdf = PDDocument.load(new java.io.File(fileLoc));
            PDFDomTree parser = new PDFDomTree();
            String dom = parser.getText(pdf);
            Document doc = Jsoup.parse(dom);
            Elements e = doc.getElementsByClass("p");
            this.string = e.text();
            //getting the html without text (paragraph) and convert the html into an image
//            saveImage(e.remove().text(),"/path/to/destination");
            extractImage(fileLoc, savingDir);
        }

        public String getString() {
            return string;
        }

        public String getImageUri() {
            return imageUri;
        }

        /**
         * save the html as image file
         *
         * @param html
         * @param path
         * @throws IOException
         */
        public void saveImage(String html, String path) throws IOException {
            HtmlImageGenerator imageGenerator = new HtmlImageGenerator();
//            System.out.println(html);
//            html = "<html><head></head><body><h1>llklkjlk</h1>"
//                    + "</body></html>";
            imageGenerator.loadHtml(html);
            BufferedImage image = imageGenerator.getBufferedImage();
            Graphics2D g = image.createGraphics();
            File f = new File(path + "/Image"
                    + Calendar.getInstance().getTime().getTime()
                    + ".png");

            ImageIO.write(image, "png", f);

            this.imageUri = f.getAbsolutePath();

        }

        /**
         * convert pdf file into an image , save the image , assign the name of
         * the image into imageUri variable
         *
         * @supports currently supports for one page pdf
         * @todo implement to handle multiple pages just to make imageUri to an
         * array
         * @param sourceDir -> uri of the pdf file
         * @param destinationDir -> destination directory
         * @throws IOException
         */
        public void extractImage(String sourceDir, String destinationDir) throws
                IOException {

            File sourceFile = new File(sourceDir);
            File destinationFile = new File(destinationDir);
            if (!destinationFile.exists()) {
                destinationFile.mkdir();
                System.out.println("Folder Created -> " + destinationFile.getAbsolutePath());
            }
            if (sourceFile.exists()) {
                PDDocument document = PDDocument.load(sourceDir);
                List<PDPage> list = document.getDocumentCatalog().getAllPages();

                String fileName = sourceFile.getName().replace(".pdf", "");
                int pageNumber = 1;
                for (PDPage page : list) {
                    BufferedImage image = page.convertToImage();
                    File outputfile = new File(destinationDir + fileName + "_" + pageNumber + "_"
                            + Calendar.getInstance().getTime().getTime() + ".png");
                    ImageIO.write(image, "png", outputfile);
                    this.imageUri = outputfile.getName();
                    pageNumber++;
                }
                document.close();
            } else {
                System.err.println(sourceFile.getName() + " File not exists");
            }
        }
    }
}
